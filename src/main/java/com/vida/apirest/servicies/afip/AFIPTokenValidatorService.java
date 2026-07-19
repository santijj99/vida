package com.vida.apirest.servicies.afip;

import com.vida.apirest.config.AfipProperties;
import com.vida.apirest.dto.afip.TokenValidationResponse;
import com.vida.apirest.utils.AFIPTokenLoader;
import com.vida.apirest.utils.AfipTaAlreadyAuthenticatedException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AFIPTokenValidatorService {

    private static final Logger log = LoggerFactory.getLogger(AFIPTokenValidatorService.class);
    private static final String SERVICE_NAME = "wsfe";

    private final AfipProperties afipProperties;
    private final AfipContextService afipContextService;
    private final AfipTaStorageService afipTaStorageService;
    private final WSAAService wsaaService;

    public TokenValidationResponse consultarEstadoToken() {
        return consultarEstadoToken(null);
    }

    public TokenValidationResponse consultarEstadoToken(Long empresaId) {
        if (!afipProperties.isEnabled()) {
            return inactivo("Módulo AFIP deshabilitado en el servidor", false);
        }

        try {
            AfipContext context = resolverContexto(empresaId);
            prepararTaHomologacion(context);
            afipTaStorageService.limpiarTaCorruptoEnBaseDeDatos(context.empresaId(), SERVICE_NAME);
            return consultarEstadoParaContexto(context, false);
        } catch (Exception e) {
            return inactivo("Error al leer token: " + e.getMessage(), false);
        }
    }

    public TokenValidationResponse validarYRegenerarToken() {
        return validarYRegenerarToken(null);
    }

    public TokenValidationResponse validarYRegenerarToken(Long empresaId) {
        if (!afipProperties.isEnabled()) {
            return TokenValidationResponse.builder()
                    .activo(true)
                    .mensaje("Módulo AFIP deshabilitado")
                    .regenerado(false)
                    .build();
        }

        try {
            AfipContext context = resolverContexto(empresaId);
            prepararTaHomologacion(context);
            afipTaStorageService.limpiarTaCorruptoEnBaseDeDatos(context.empresaId(), SERVICE_NAME);

            // En homologación siempre intentamos WSAA (como wsaa-client.php en cada ejecución).
            if (context.homologacion()) {
                return regenerarToken(context, "Homologación: solicitando TA.xml a AFIP con Java...");
            }

            TokenValidationResponse estado;
            try {
                estado = consultarEstadoParaContexto(context, false);
            } catch (Exception e) {
                log.warn("TA local inválido para empresa {}: {}. Se regenerará con WSAA.",
                        context.empresaId(), e.getMessage());
                return regenerarToken(context, "TA.xml inválido o corrupto. Generando TA.xml con Java...");
            }
            if (estado.isActivo()) {
                return estado;
            }
            return regenerarToken(context, "Token AFIP vencido o ausente. Generando TA.xml con Java...");
        } catch (Exception e) {
            log.error("Error al validar token AFIP: {}", e.getMessage());
            return inactivo("Error al validar token: " + e.getMessage(), false);
        }
    }

    private void prepararTaHomologacion(AfipContext context) {
        afipTaStorageService.restaurarTaEnDisco(context, SERVICE_NAME);
        if (context.homologacion()) {
            afipTaStorageService.importarTaAlternativoSiFalta(context, SERVICE_NAME);
        }
    }

    private TokenValidationResponse consultarEstadoParaContexto(AfipContext context, boolean regenerado) throws Exception {
        Optional<AFIPTokenLoader.TokenSign> tokenOpt = afipTaStorageService.cargarToken(context, SERVICE_NAME);
        if (tokenOpt.isEmpty()) {
            return inactivo("No hay TA.xml. Usá «Verificar / Regenerar» para que Java lo genere automáticamente.", regenerado);
        }

        AFIPTokenLoader.TokenSign tokenSign = tokenOpt.get();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date expiration = tokenSign.getExpiration();

        if (tokenSign.isValid()) {
            return respuestaActiva(tokenSign, "Token activo. Expira el "
                    + (expiration != null ? sdf.format(expiration) : "desconocido"), regenerado);
        }

        if (context.homologacion()) {
            return respuestaActiva(tokenSign,
                    "Homologación: TA.xml presente (fecha local "
                            + (expiration != null ? "expiró el " + sdf.format(expiration) : "sin fecha")
                            + "). AFIP puede seguir aceptándolo; probá facturar o regenerar.",
                    regenerado);
        }

        return TokenValidationResponse.builder()
                .activo(false)
                .mensaje("Token vencido"
                        + (expiration != null ? ". Expiró el " + sdf.format(expiration) : ""))
                .expirationTime(expiration)
                .regenerado(regenerado)
                .build();
    }

    private TokenValidationResponse regenerarToken(AfipContext context, String motivo) {
        try {
            afipContextService.validarCertificados(context);
            log.info("Generando TA.xml con Java WSAA para empresa {} (homologación={})",
                    context.empresaId(), context.homologacion());

            AFIPTokenLoader.TokenSign tokenSign = afipContextService.callWithContext(
                    context,
                    () -> afipTaStorageService.generarYGuardar(context, SERVICE_NAME, context.homologacion())
            );
            wsaaService.limpiarCache();

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Path certDir = context.certificadosDir();
            Date expiration = tokenSign.getExpiration();
            String expMsg = expiration != null ? sdf.format(expiration) : "desconocido";

            return respuestaActiva(tokenSign,
                    motivo + " TA.xml disponible en " + certDir + ". Expira el " + expMsg,
                    true);
        } catch (AfipTaAlreadyAuthenticatedException e) {
            log.info("AFIP reporta TA vigente para empresa {}: {}", context.empresaId(), e.getMessage());
            return reutilizarTaExistente(context);
        } catch (Exception e) {
            log.error("Error al regenerar token AFIP: {}", e.getMessage(), e);
            return inactivo(motivo + " " + e.getMessage(), true);
        }
    }

    private TokenValidationResponse reutilizarTaExistente(AfipContext context) {
        prepararTaHomologacion(context);
        try {
            Optional<AFIPTokenLoader.TokenSign> tokenOpt = afipTaStorageService.cargarToken(context, SERVICE_NAME);
            if (tokenOpt.isPresent()) {
                return respuestaActiva(tokenOpt.get(),
                        "AFIP confirma que el token sigue vigente. Java reutiliza el TA.xml local "
                                + "(mismo comportamiento que cuando PHP ya tenía sesión activa en AFIP).",
                        false);
            }
        } catch (Exception e) {
            return inactivo("No se pudo leer el TA local: " + e.getMessage(), true);
        }

        return inactivo(
                "AFIP tiene un token vigente para este certificado y aún no libera uno nuevo. "
                        + "Java reintentará automáticamente en homologación. "
                        + "Si persiste, esperá unas horas y volvé a pulsar «Verificar / Regenerar».",
                true);
    }

    private TokenValidationResponse respuestaActiva(
            AFIPTokenLoader.TokenSign tokenSign, String mensaje, boolean regenerado) {
        return TokenValidationResponse.builder()
                .activo(true)
                .mensaje(mensaje)
                .expirationTime(tokenSign.getExpiration())
                .regenerado(regenerado)
                .build();
    }

    private AfipContext resolverContexto(Long empresaId) {
        if (empresaId != null) {
            return afipContextService.resolveForEmpresaId(empresaId);
        }
        Optional<Long> currentEmpresa = afipContextService.resolveEmpresaIdForCurrentUser();
        if (currentEmpresa.isPresent()) {
            return afipContextService.resolveForEmpresaId(currentEmpresa.get());
        }
        return afipContextService.resolveAllHabilitadas().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No hay empresas con ARCA habilitado. Configurá AFIP en el módulo de empresas."));
    }

    private TokenValidationResponse inactivo(String mensaje, boolean regenerado) {
        return TokenValidationResponse.builder()
                .activo(false)
                .mensaje(mensaje)
                .regenerado(regenerado)
                .build();
    }
}
