package com.vida.apirest.servicies.afip;

import com.vida.apirest.config.AfipProperties;
import com.vida.apirest.utils.AFIPTokenLoader;
import com.vida.apirest.utils.AfipTokenPathResolver;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

@Service
@RequiredArgsConstructor
public class WSAAService {

    private static final Logger log = LoggerFactory.getLogger(WSAAService.class);
    private static final String WSAA_URL_HOMOLOGACION = "https://wsaahomo.afip.gov.ar/ws/services/LoginCms";
    private static final String WSAA_URL_PRODUCCION = "https://wsaa.afip.gov.ar/ws/services/LoginCms";
    private static final String SERVICE = "wsfe";

    private final AfipProperties afipProperties;
    private final ReentrantLock lock = new ReentrantLock();
    private final Map<String, TokenCache> tokenCache = new HashMap<>();

    @PostConstruct
    void init() {
        if (afipProperties.getCertPath() != null && !afipProperties.getCertPath().isBlank()) {
            log.info("AFIP WSAA configurado con certificado digital");
        } else if (afipProperties.getTokenXmlPath() != null && !afipProperties.getTokenXmlPath().isBlank()) {
            log.info("AFIP WSAA configurado con token XML: {}", afipProperties.getTokenXmlPath());
        }
    }

    public void limpiarCache() {
        lock.lock();
        try {
            tokenCache.clear();
        } finally {
            lock.unlock();
        }
    }

    public TokenSign obtenerTokenSign() throws Exception {
        String tokenLoadPath = AfipTokenPathResolver.resolveLoadPath(afipProperties);
        if (tokenLoadPath != null && !tokenLoadPath.isBlank()) {
            try {
                AFIPTokenLoader.TokenSign tokenSign = AFIPTokenLoader.loadFromXml(tokenLoadPath);
                if (tokenSign.isValid()) {
                    return new TokenSign(tokenSign.getToken(), tokenSign.getSign(), tokenSign.getExpiration());
                }
                log.warn("Token AFIP expirado ({})", tokenLoadPath);
            } catch (Exception e) {
                log.error("No se pudo cargar token desde {}: {}", tokenLoadPath, e.getMessage());
            }
        }

        String cacheKey = afipProperties.getCuit() + "_" + SERVICE;
        lock.lock();
        try {
            TokenCache cache = tokenCache.get(cacheKey);
            if (cache != null && cache.isValid()) {
                return cache.getTokenSign();
            }

            TokenSign tokenSign = loginCms();
            tokenCache.put(cacheKey, new TokenCache(tokenSign));
            return tokenSign;
        } finally {
            lock.unlock();
        }
    }

    private TokenSign loginCms() throws Exception {
        if (afipProperties.getCertPath() == null || afipProperties.getCertPath().isBlank()) {
            throw new IllegalStateException(
                    "AFIP no está configurado. Defina afip.token-xml-path o afip.cert-path en application.yml");
        }

        throw new UnsupportedOperationException(
                "Login CMS con certificado no está habilitado. Use afip.token-xml-path o el script PHP de regeneración.");
    }

    private String enviarLoginCms(String cmsBase64) throws Exception {
        String url = afipProperties.isHomologacion() ? WSAA_URL_HOMOLOGACION : WSAA_URL_PRODUCCION;

        String soapRequest = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:wsaa=\"https://wsaa.afip.gov.ar/ws/services/LoginCms\">\n"
                + "  <soapenv:Header/>\n"
                + "  <soapenv:Body>\n"
                + "    <wsaa:loginCms>\n"
                + "      <wsaa:in0>" + cmsBase64 + "</wsaa:in0>\n"
                + "    </wsaa:loginCms>\n"
                + "  </soapenv:Body>\n"
                + "</soapenv:Envelope>";

        java.net.URL wsaaUrl = new java.net.URL(url);
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) wsaaUrl.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        conn.setRequestProperty("SOAPAction", "");
        conn.setDoOutput(true);

        if (afipProperties.isHomologacion()) {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
            }, new java.security.SecureRandom());
            javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        }

        try (java.io.OutputStream os = conn.getOutputStream()) {
            os.write(soapRequest.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        StringBuilder response = new StringBuilder();
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(conn.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }

        return response.toString();
    }

    private TokenSign parsearRespuesta(String respuesta) throws Exception {
        int inicio = respuesta.indexOf("<loginCmsResponse>");
        int fin = respuesta.indexOf("</loginCmsResponse>");
        if (inicio == -1 || fin == -1) {
            throw new Exception("Respuesta inválida de WSAA");
        }

        String contenido = respuesta.substring(inicio + 18, fin);
        byte[] decoded = Base64.getDecoder().decode(contenido.trim());
        String xml = new String(decoded, java.nio.charset.StandardCharsets.UTF_8);

        int tokenInicio = xml.indexOf("<token>");
        int tokenFin = xml.indexOf("</token>");
        int signInicio = xml.indexOf("<sign>");
        int signFin = xml.indexOf("</sign>");

        if (tokenInicio == -1 || tokenFin == -1 || signInicio == -1 || signFin == -1) {
            throw new Exception("No se pudo parsear token y sign de la respuesta WSAA");
        }

        String token = xml.substring(tokenInicio + 7, tokenFin);
        String sign = xml.substring(signInicio + 6, signFin);

        Date expiration = null;
        int expInicio = xml.indexOf("<expirationTime>");
        int expFin = xml.indexOf("</expirationTime>");
        if (expInicio != -1 && expFin != -1) {
            String expStr = xml.substring(expInicio + 17, expFin);
            expiration = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").parse(expStr);
        }

        return new TokenSign(token, sign, expiration);
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
    }
}
