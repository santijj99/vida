package com.vida.apirest.servicies.afip;

import com.vida.apirest.config.AfipProperties;
import com.vida.apirest.dto.afip.AfipAmbienteResponse;
import com.vida.apirest.utils.AfipTokenPathResolver;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "afip.enabled", havingValue = "true")
public class AfipConfigService {

    private static final Logger log = LoggerFactory.getLogger(AfipConfigService.class);

    private static final String WSAA_HOMO = "https://wsaahomo.afip.gov.ar/ws/services/LoginCms";
    private static final String WSAA_PROD = "https://wsaa.afip.gov.ar/ws/services/LoginCms";
    private static final String WSFE_HOMO = "https://wswhomo.afip.gov.ar/wsfev1/service.asmx";
    private static final String WSFE_PROD = "https://servicios1.afip.gov.ar/wsfev1/service.asmx";

    private final AfipProperties afipProperties;
    private final WSAAService wsaaService;

    public AfipAmbienteResponse consultarAmbiente() {
        return construirRespuesta(null);
    }

    public AfipAmbienteResponse cambiarAmbiente(boolean homologacion) {
        boolean anterior = afipProperties.isHomologacion();
        afipProperties.setHomologacion(homologacion);
        wsaaService.limpiarCache();

        String mensaje = homologacion
                ? "Ambiente cambiado a Homologación. Regenerá el token TA.xml con certificado de testing."
                : "Ambiente cambiado a Producción. Regenerá el token TA.xml con certificado de producción.";

        if (anterior != homologacion) {
            log.info("AFIP ambiente: {} ��� {}", anterior ? "Homologación" : "Producción",
                    homologacion ? "Homologación" : "Producción");
        }

        return construirRespuesta(mensaje);
    }

    private AfipAmbienteResponse construirRespuesta(String mensaje) {
        boolean homo = afipProperties.isHomologacion();
        java.io.File certDir = AfipTokenPathResolver.resolveCertificadosDir(afipProperties);
        return AfipAmbienteResponse.builder()
                .homologacion(homo)
                .ambiente(homo ? "Homologación" : "Producción")
                .wsaaUrl(homo ? WSAA_HOMO : WSAA_PROD)
                .wsfeUrl(homo ? WSFE_HOMO : WSFE_PROD)
                .certificadosDir(certDir != null ? certDir.getAbsolutePath() : null)
                .phpScriptPath(AfipTokenPathResolver.resolvePhpScriptPath(afipProperties))
                .mensaje(mensaje)
                .build();
    }
}
