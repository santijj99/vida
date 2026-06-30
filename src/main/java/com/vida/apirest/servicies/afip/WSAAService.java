package com.vida.apirest.servicies.afip;

import com.vida.apirest.utils.AFIPTokenLoader;
import com.vida.apirest.utils.AfipTaAlreadyAuthenticatedException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class WSAAService {

    private static final Logger log = LoggerFactory.getLogger(WSAAService.class);
    private static final String SERVICE_WSFE = "wsfe";

    private final AfipContextService afipContextService;
    private final AfipTaStorageService afipTaStorageService;
    private final ReentrantLock lock = new ReentrantLock();
    private final Map<String, TokenCache> tokenCache = new HashMap<>();

    public void limpiarCache() {
        lock.lock();
        try {
            tokenCache.clear();
        } finally {
            lock.unlock();
        }
    }

    public TokenSign obtenerTokenSign() throws Exception {
        return obtenerTokenSign(SERVICE_WSFE);
    }

    public TokenSign obtenerTokenSign(String service) throws Exception {
        AfipContext context = AfipContextHolder.require();
        afipContextService.validarCertificados(context);
        if (context.homologacion()) {
            afipTaStorageService.importarTaAlternativoSiFalta(context, service);
        }
        afipTaStorageService.restaurarTaEnDisco(context, service);

        String cacheKey = context.empresaId() + "_" + service;
        lock.lock();
        try {
            TokenCache cache = tokenCache.get(cacheKey);
            if (cache != null && cache.isValid()) {
                return cache.getTokenSign();
            }

            TokenSign tokenSign = cargarOGenerarToken(context, service);
            tokenCache.put(cacheKey, new TokenCache(tokenSign));
            return tokenSign;
        } finally {
            lock.unlock();
        }
    }

    private TokenSign cargarOGenerarToken(AfipContext context, String service) throws Exception {
        Optional<AFIPTokenLoader.TokenSign> existente = afipTaStorageService.cargarToken(context, service);
        if (existente.isPresent()) {
            AFIPTokenLoader.TokenSign ts = existente.get();
            if (ts.isValid() || context.homologacion()) {
                return new TokenSign(ts.getToken(), ts.getSign(), ts.getExpiration());
            }
            Path taPath = afipTaStorageService.resolverRutaTa(context, service);
            log.warn("Token AFIP con fecha vencida en {}, regenerando con Java...", taPath);
        } else {
            log.info("TA.xml ausente para empresa {}, generando con Java WSAA...", context.empresaId());
        }

        try {
            AFIPTokenLoader.TokenSign generado = afipTaStorageService.generarYGuardar(
                    context, service, context.homologacion());
            return new TokenSign(generado.getToken(), generado.getSign(), generado.getExpiration());
        } catch (AfipTaAlreadyAuthenticatedException e) {
            if (afipTaStorageService.restaurarTaEnDisco(context, service)) {
                AFIPTokenLoader.TokenSign restaurado = afipTaStorageService.cargarToken(context, service)
                        .orElseThrow(() -> new IllegalStateException("No se pudo cargar el TA restaurado"));
                return new TokenSign(restaurado.getToken(), restaurado.getSign(), restaurado.getExpiration());
            }
            throw new IllegalStateException(
                    "AFIP tiene un token vigente para este certificado. Esperá a que expire para que Java genere un nuevo TA.xml.",
                    e);
        }
    }

    private static class TokenCache {
        private final TokenSign tokenSign;
        private final Date expiration;

        TokenCache(TokenSign tokenSign) {
            this.tokenSign = tokenSign;
            this.expiration = tokenSign.getExpiration() != null
                    ? tokenSign.getExpiration()
                    : new Date(System.currentTimeMillis() + 3_600_000L);
        }

        boolean isValid() {
            return expiration.getTime() > (System.currentTimeMillis() + 300_000L);
        }

        TokenSign getTokenSign() {
            return tokenSign;
        }
    }

    public static class TokenSign {
        private final String token;
        private final String sign;
        private final Date expiration;

        public TokenSign(String token, String sign, Date expiration) {
            this.token = token;
            this.sign = sign;
            this.expiration = expiration;
        }

        public String getToken() {
            return token;
        }

        public String getSign() {
            return sign;
        }

        public Date getExpiration() {
            return expiration;
        }

        public String formatExpiration() {
            if (expiration == null) {
                return "desconocido";
            }
            return new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(expiration);
        }
    }
}
