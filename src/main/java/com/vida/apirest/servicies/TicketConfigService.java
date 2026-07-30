package com.vida.apirest.servicies;

import com.vida.apirest.dto.ticket.TicketConfigRequest;
import com.vida.apirest.dto.ticket.TicketConfigResponse;
import com.vida.apirest.model.empresa.Empresa;
import com.vida.apirest.model.empresa.EmpresaTicketConfig;
import com.vida.apirest.model.empresa.FormatoTicketPdf;
import com.vida.apirest.repositories.EmpresaRepository;
import com.vida.apirest.repositories.EmpresaTicketConfigRepository;
import com.vida.apirest.servicies.afip.AfipContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketConfigService {

    private final EmpresaTicketConfigRepository configRepository;
    private final EmpresaRepository empresaRepository;
    private final AfipContextService afipContextService;

    @Transactional(readOnly = true)
    public TicketConfigResponse obtener(Long empresaId) {
        Long resolved = resolverEmpresaId(empresaId);
        return toResponse(obtenerODefault(resolved));
    }

    @Transactional
    public TicketConfigResponse guardar(TicketConfigRequest request) {
        Long empresaId = resolverEmpresaId(request.getEmpresaId());
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));

        EmpresaTicketConfig config = configRepository.findByEmpresaId(empresaId)
                .orElseGet(() -> {
                    EmpresaTicketConfig nuevo = new EmpresaTicketConfig();
                    nuevo.setEmpresa(empresa);
                    nuevo.setAbrirAutomaticamente(true);
                    return nuevo;
                });

        if (request.getFormato() != null && !request.getFormato().isBlank()) {
            config.setFormato(parseFormato(request.getFormato()));
        }
        if (request.getAbrirAutomaticamente() != null) {
            config.setAbrirAutomaticamente(request.getAbrirAutomaticamente());
        }

        return toResponse(configRepository.save(config));
    }

    @Transactional(readOnly = true)
    public FormatoTicketPdf resolverFormato(Long empresaId) {
        if (empresaId == null) {
            return FormatoTicketPdf.TERMICO_80MM;
        }
        return configRepository.findByEmpresaId(empresaId)
                .map(EmpresaTicketConfig::getFormato)
                .orElse(FormatoTicketPdf.TERMICO_80MM);
    }

    @Transactional(readOnly = true)
    public boolean resolverAbrirAutomaticamente(Long empresaId) {
        if (empresaId == null) {
            return true;
        }
        return configRepository.findByEmpresaId(empresaId)
                .map(c -> Boolean.TRUE.equals(c.getAbrirAutomaticamente()))
                .orElse(true);
    }

    private EmpresaTicketConfig obtenerODefault(Long empresaId) {
        return configRepository.findByEmpresaId(empresaId).orElseGet(() -> {
            EmpresaTicketConfig def = new EmpresaTicketConfig();
            def.setFormato(FormatoTicketPdf.TERMICO_80MM);
            def.setAbrirAutomaticamente(true);
            if (empresaRepository.existsById(empresaId)) {
                def.setEmpresa(empresaRepository.findById(empresaId).orElse(null));
            }
            return def;
        });
    }

    private Long resolverEmpresaId(Long empresaId) {
        if (empresaId != null) {
            return empresaId;
        }
        return afipContextService.resolveEmpresaIdForCurrentUser()
                .orElseThrow(() -> new RuntimeException("No se pudo determinar la empresa"));
    }

    private FormatoTicketPdf parseFormato(String valor) {
        try {
            return FormatoTicketPdf.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return FormatoTicketPdf.TERMICO_80MM;
        }
    }

    private TicketConfigResponse toResponse(EmpresaTicketConfig config) {
        TicketConfigResponse dto = new TicketConfigResponse();
        dto.setId(config.getId());
        if (config.getEmpresa() != null) {
            dto.setEmpresaId(config.getEmpresa().getId());
            dto.setEmpresaNombre(config.getEmpresa().getNombre());
        }
        dto.setFormato(config.getFormato() != null
                ? config.getFormato().name()
                : FormatoTicketPdf.TERMICO_80MM.name());
        dto.setAbrirAutomaticamente(config.getAbrirAutomaticamente() == null
                || Boolean.TRUE.equals(config.getAbrirAutomaticamente()));
        return dto;
    }
}
