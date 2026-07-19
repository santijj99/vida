package com.vida.apirest.servicies.afip;

import com.vida.apirest.config.AfipProperties;
import com.vida.apirest.dto.afip.AfipAmbienteResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AfipConfigService {

    private static final Logger log = LoggerFactory.getLogger(AfipConfigService.class);

    private static final String WSAA_HOMO = "https://wsaahomo.afip.gov.ar/ws/services/LoginCms";
    private static final String WSAA_PROD = "https://wsaa.afip.gov.ar/ws/services/LoginCms";
    private static final String WSFE_HOMO = "https://wswhomo.afip.gov.ar/wsfev1/service.asmx";
    private static final String WSFE_PROD = "https://servicios1.afip.gov.ar/wsfev1/service.asmx";

    private final AfipProperties afipProperties;
    private final WSAAService wsaaService;
    private final AfipContextService afipContextService;

    public AfipAmbienteResponse consultarAmbiente() {
        return construirRespuesta(null, null);
    }

    public AfipAmbienteResponse consultarAmbiente(Long empresaId) {
        AfipContext context = empresaId != null
                ? afipContextService.resolveOptionalForEmpresaId(empresaId).orElse(null)
                : afipContextService.resolveEmpresaIdForCurrentUser()
                        .flatMap(afipContextService::resolveOptionalForEmpresaId)
                        .orElse(null);
        return construirRespuesta(null, context);
    }

    public AfipAmbienteResponse cambiarAmbiente(boolean homologacion) {
        boolean anterior = afipProperties.isHomologacion();
        afipProperties.setHomologacion(homologacion);
        wsaaService.limpiarCache();

        String mensaje = homologacion
                ? "Ambiente cambiado a Homologaci?n. Regener? el token con el certificado de testing."
                : "Ambiente cambiado a Producci?n. Regener? el token con el certificado de producci?n.";

        if (anterior != homologacion) {
            log.info("AFIP ambiente: {} -> {}", anterior ? "Homologaci?n" : "Producci?n",
                    homologacion ? "Homologaci?n" : "Producci?n");
        }

        return construirRespuesta(mensaje, null);
    }

    private AfipAmbienteResponse construirRespuesta(String mensaje, AfipContext context) {
        boolean homo = afipProperties.isHomologacion();
        return AfipAmbienteResponse.builder()
                .homologacion(homo)
                .ambiente(homo ? "Homologaci?n" : "Producci?n")
                .wsaaUrl(homo ? WSAA_HOMO : WSAA_PROD)
                .wsfeUrl(homo ? WSFE_HOMO : WSFE_PROD)
                .certificadosDir(context != null ? context.certificadosDir().toString() : null)
                .empresaId(context != null ? context.empresaId() : null)
                .cuit(context != null ? context.cuit() : null)
                .razonSocial(context != null ? context.razonSocial() : null)
                .mensaje(mensaje)
                .build();
    }
}
