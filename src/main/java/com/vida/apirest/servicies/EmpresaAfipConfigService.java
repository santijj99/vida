package com.vida.apirest.servicies;

import com.vida.apirest.dto.empresa.EmpresaAfipConfigRequest;
import com.vida.apirest.dto.empresa.EmpresaAfipConfigResponse;
import com.vida.apirest.model.empresa.Empresa;
import com.vida.apirest.model.empresa.EmpresaAfipConfig;
import com.vida.apirest.repositories.EmpresaAfipConfigRepository;
import com.vida.apirest.repositories.EmpresaRepository;
import com.vida.apirest.servicies.afip.AfipContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class EmpresaAfipConfigService {

    private final EmpresaRepository empresaRepository;
    private final EmpresaAfipConfigRepository empresaAfipConfigRepository;
    private final AfipContextService afipContextService;

    @Transactional(readOnly = true)
    public EmpresaAfipConfigResponse obtener(Long empresaId) {
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));
        EmpresaAfipConfig config = empresaAfipConfigRepository.findByEmpresaId(empresaId)
                .orElseGet(() -> configPorDefecto(empresa));
        return toResponse(empresa, config);
    }

    @Transactional
    public EmpresaAfipConfigResponse guardar(Long empresaId, EmpresaAfipConfigRequest request) {
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));

        EmpresaAfipConfig config = empresaAfipConfigRepository.findByEmpresaId(empresaId)
                .orElseGet(() -> {
                    EmpresaAfipConfig nuevo = new EmpresaAfipConfig();
                    nuevo.setEmpresa(empresa);
                    return nuevo;
                });

        if (request.getAfipHabilitado() != null) {
            config.setAfipHabilitado(request.getAfipHabilitado());
        }
        if (request.getPtoVta() != null) {
            config.setPtoVta(request.getPtoVta());
        }
        if (request.getCbteTipoDefault() != null) {
            config.setCbteTipoDefault(request.getCbteTipoDefault());
        }
        if (request.getCondicionIva() != null) {
            config.setCondicionIva(request.getCondicionIva());
        }
        if (request.getIibb() != null) {
            config.setIibb(request.getIibb());
        }
        if (request.getInicioActividad() != null) {
            config.setInicioActividad(request.getInicioActividad());
        }
        if (request.getCertificadosDirectorio() != null) {
            config.setCertificadosDirectorio(request.getCertificadosDirectorio().isBlank()
                    ? null
                    : request.getCertificadosDirectorio().trim());
        }
        if (request.getClavePrivadaPassword() != null) {
            config.setClavePrivadaPassword(request.getClavePrivadaPassword().isBlank()
                    ? null
                    : request.getClavePrivadaPassword());
        }

        EmpresaAfipConfig guardada = empresaAfipConfigRepository.save(config);
        return toResponse(empresa, guardada);
    }

    private EmpresaAfipConfig configPorDefecto(Empresa empresa) {
        EmpresaAfipConfig config = new EmpresaAfipConfig();
        config.setEmpresa(empresa);
        config.setAfipHabilitado(false);
        config.setPtoVta(1);
        config.setCbteTipoDefault(6);
        config.setCondicionIva("IVA Responsable Inscripto");
        return config;
    }

    private EmpresaAfipConfigResponse toResponse(Empresa empresa, EmpresaAfipConfig config) {
        Path certDir = afipContextService.resolveCertificadosDir(config);
        boolean certsOk = Files.isRegularFile(certDir.resolve("certificado.crt"))
                && Files.isRegularFile(certDir.resolve("MiClavePrivada.key"));

        return EmpresaAfipConfigResponse.builder()
                .empresaId(empresa.getId())
                .empresaNombre(empresa.getNombre())
                .cuit(empresa.getCuit())
                .razonSocial(empresa.getRazonSocial())
                .domicilio(empresa.getDomicilio())
                .afipHabilitado(config.isAfipHabilitado())
                .ptoVta(config.getPtoVta())
                .cbteTipoDefault(config.getCbteTipoDefault())
                .condicionIva(config.getCondicionIva())
                .iibb(config.getIibb())
                .inicioActividad(config.getInicioActividad())
                .certificadosDirectorio(certDir.toString())
                .certificadosPresentes(certsOk)
                .build();
    }
}
