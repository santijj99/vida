package com.vida.apirest.servicies.afip;

import com.vida.apirest.config.AfipProperties;
import com.vida.apirest.dto.afip.AfipAmbienteResponse;
import com.vida.apirest.model.empresa.EmpresaAfipConfig;
import com.vida.apirest.repositories.EmpresaAfipConfigRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final EmpresaAfipConfigRepository empresaAfipConfigRepository;

    public AfipAmbienteResponse consultarAmbiente() {
        return consultarAmbiente(null);
    }

    @Transactional
    public AfipAmbienteResponse consultarAmbiente(Long empresaId) {
        AfipContext context = null;
        Boolean homologacionEmpresa = null;

        if (empresaId != null) {
            EmpresaAfipConfig config = empresaAfipConfigRepository.findByEmpresaIdWithEmpresa(empresaId).orElse(null);
            if (config != null) {
                homologacionEmpresa = config.isHomologacion();
                afipProperties.setHomologacion(config.isHomologacion());
                if (config.isAfipHabilitado()) {
                    context = afipContextService.resolveOptionalForEmpresaId(empresaId).orElse(null);
                }
            }
        } else {
            context = afipContextService.resolveEmpresaIdForCurrentUser()
                    .flatMap(afipContextService::resolveOptionalForEmpresaId)
                    .orElse(null);
            if (context != null) {
                homologacionEmpresa = context.homologacion();
                afipProperties.setHomologacion(context.homologacion());
            }
        }

        return construirRespuesta(null, context, homologacionEmpresa);
    }

    @Transactional
    public AfipAmbienteResponse cambiarAmbiente(boolean homologacion) {
        return cambiarAmbiente(homologacion, null);
    }

    @Transactional
    public AfipAmbienteResponse cambiarAmbiente(boolean homologacion, Long empresaId) {
        boolean anterior = afipProperties.isHomologacion();
        afipProperties.setHomologacion(homologacion);
        wsaaService.limpiarCache();

        Long targetEmpresaId = empresaId;
        if (targetEmpresaId == null) {
            targetEmpresaId = afipContextService.resolveEmpresaIdForCurrentUser().orElse(null);
        }

        AfipContext context = null;
        if (targetEmpresaId != null) {
            EmpresaAfipConfig config = empresaAfipConfigRepository.findByEmpresaIdWithEmpresa(targetEmpresaId)
                    .orElse(null);
            if (config != null) {
                config.setHomologacion(homologacion);
                empresaAfipConfigRepository.save(config);
                if (config.isAfipHabilitado()) {
                    context = afipContextService.resolveOptionalForEmpresaId(targetEmpresaId).orElse(null);
                }
            }
        }

        String mensaje = homologacion
                ? "Ambiente cambiado a Homologación. Regenerá el token con el certificado de testing."
                : "Ambiente cambiado a Producción. Regenerá el token con el certificado de producción.";

        if (anterior != homologacion) {
            log.info("AFIP ambiente empresa {}: {} -> {}",
                    targetEmpresaId,
                    anterior ? "Homologación" : "Producción",
                    homologacion ? "Homologación" : "Producción");
        }

        return construirRespuesta(mensaje, context, homologacion);
    }

    private AfipAmbienteResponse construirRespuesta(
            String mensaje,
            AfipContext context,
            Boolean homologacionOverride
    ) {
        boolean homo = homologacionOverride != null
                ? homologacionOverride
                : (context != null ? context.homologacion() : afipProperties.isHomologacion());
        return AfipAmbienteResponse.builder()
                .homologacion(homo)
                .ambiente(homo ? "Homologación" : "Producción")
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
