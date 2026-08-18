package com.vida.apirest.servicies;

import com.vida.apirest.dto.credito.CreditoConfigRequest;
import com.vida.apirest.dto.credito.CreditoConfigResponse;
import com.vida.apirest.model.credito.CreditoConfigEmpresa;
import com.vida.apirest.model.empresa.Empresa;
import com.vida.apirest.repositories.CreditoConfigEmpresaRepository;
import com.vida.apirest.repositories.EmpresaRepository;
import com.vida.apirest.servicies.afip.AfipContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CreditoConfigService {

    private final CreditoConfigEmpresaRepository configRepository;
    private final EmpresaRepository empresaRepository;
    private final AfipContextService afipContextService;

    @Transactional(readOnly = true)
    public CreditoConfigResponse obtener(Long empresaId) {
        Long resolved = resolverEmpresaId(empresaId);
        CreditoConfigEmpresa config = obtenerODefault(resolved);
        return toResponse(config);
    }

    @Transactional
    public CreditoConfigResponse guardar(CreditoConfigRequest request) {
        Long empresaId = resolverEmpresaId(request.getEmpresaId());
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));

        CreditoConfigEmpresa config = configRepository.findByEmpresaId(empresaId)
                .orElseGet(() -> {
                    CreditoConfigEmpresa nuevo = new CreditoConfigEmpresa();
                    nuevo.setEmpresa(empresa);
                    return nuevo;
                });

        if (request.getDiasGracia() != null) {
            config.setDiasGracia(Math.max(0, request.getDiasGracia()));
        }
        if (request.getPorcentajeMora() != null) {
            config.setPorcentajeMora(request.getPorcentajeMora().max(BigDecimal.ZERO));
        }
        if (request.getTipoInteres() != null && !request.getTipoInteres().isBlank()) {
            config.setTipoInteres(parseTipoInteres(request.getTipoInteres()));
        }
        if (request.getModoDiaVencimiento() != null && !request.getModoDiaVencimiento().isBlank()) {
            config.setModoDiaVencimiento(parseModoDia(request.getModoDiaVencimiento()));
        }

        config = configRepository.save(config);
        return toResponse(config);
    }

    @Transactional(readOnly = true)
    public CreditoConfigEmpresa obtenerODefault(Long empresaId) {
        if (empresaId == null) {
            return crearDefaultSinEmpresa();
        }
        return configRepository.findByEmpresaId(empresaId).orElseGet(() -> {
            CreditoConfigEmpresa def = crearDefaultSinEmpresa();
            if (empresaRepository.existsById(empresaId)) {
                Empresa empresa = empresaRepository.findById(empresaId).orElse(null);
                def.setEmpresa(empresa);
            }
            return def;
        });
    }

    private CreditoConfigEmpresa crearDefaultSinEmpresa() {
        CreditoConfigEmpresa config = new CreditoConfigEmpresa();
        config.setDiasGracia(0);
        config.setPorcentajeMora(BigDecimal.TEN);
        config.setTipoInteres(CreditoConfigEmpresa.TipoInteresMora.FIJO);
        config.setModoDiaVencimiento(CreditoConfigEmpresa.ModoDiaVencimiento.DIA_10);
        return config;
    }

    private Long resolverEmpresaId(Long empresaId) {
        if (empresaId != null) {
            return empresaId;
        }
        return afipContextService.resolveEmpresaIdForCurrentUser()
                .orElseThrow(() -> new RuntimeException(
                        "No hay empresas en esta cuenta. Creá una en Organización → Empresas."));
    }

    private CreditoConfigResponse toResponse(CreditoConfigEmpresa config) {
        CreditoConfigResponse dto = new CreditoConfigResponse();
        dto.setId(config.getId());
        if (config.getEmpresa() != null) {
            dto.setEmpresaId(config.getEmpresa().getId());
            dto.setEmpresaNombre(config.getEmpresa().getNombre());
        }
        dto.setDiasGracia(config.getDiasGracia());
        dto.setPorcentajeMora(config.getPorcentajeMora());
        dto.setTipoInteres(config.getTipoInteres() != null ? config.getTipoInteres().name() : CreditoConfigEmpresa.TipoInteresMora.FIJO.name());
        dto.setModoDiaVencimiento(config.getModoDiaVencimiento() != null
                ? config.getModoDiaVencimiento().name()
                : CreditoConfigEmpresa.ModoDiaVencimiento.DIA_10.name());
        return dto;
    }

    private CreditoConfigEmpresa.TipoInteresMora parseTipoInteres(String valor) {
        try {
            return CreditoConfigEmpresa.TipoInteresMora.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return CreditoConfigEmpresa.TipoInteresMora.FIJO;
        }
    }

    private CreditoConfigEmpresa.ModoDiaVencimiento parseModoDia(String valor) {
        try {
            return CreditoConfigEmpresa.ModoDiaVencimiento.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return CreditoConfigEmpresa.ModoDiaVencimiento.DIA_10;
        }
    }
}
