package com.vida.apirest.servicies;

import com.vida.apirest.dto.credito.CreditoHistorialResponse;
import com.vida.apirest.model.credito.Credito;
import com.vida.apirest.model.credito.CreditoHistorial;
import com.vida.apirest.repositories.CreditoHistorialRepository;
import com.vida.apirest.security.AppUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CreditoHistorialService {

    private final CreditoHistorialRepository historialRepository;

    @Transactional
    public void registrar(Credito credito, String campo, String valorAnterior, String valorNuevo) {
        CreditoHistorial h = new CreditoHistorial();
        h.setCredito(credito);
        h.setCampo(campo);
        h.setValorAnterior(valorAnterior);
        h.setValorNuevo(valorNuevo);
        resolverUsuarioActual().ifPresent(u -> {
            h.setUsuarioId(u.usuarioId());
            h.setUsuarioNombre(u.nombre());
        });
        historialRepository.save(h);
    }

    @Transactional(readOnly = true)
    public List<CreditoHistorialResponse> listarPorCredito(Long creditoId) {
        return historialRepository.findByCreditoIdOrderByCreatedAtDesc(creditoId).stream()
                .map(this::toResponse)
                .toList();
    }

    private CreditoHistorialResponse toResponse(CreditoHistorial h) {
        CreditoHistorialResponse dto = new CreditoHistorialResponse();
        dto.setId(h.getId());
        dto.setCreditoId(h.getCredito().getId());
        dto.setCampo(h.getCampo());
        dto.setValorAnterior(h.getValorAnterior());
        dto.setValorNuevo(h.getValorNuevo());
        dto.setUsuarioId(h.getUsuarioId());
        dto.setUsuarioNombre(h.getUsuarioNombre());
        dto.setCreatedAt(h.getCreatedAt());
        return dto;
    }

    private java.util.Optional<UsuarioRef> resolverUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUserDetails details)) {
            return java.util.Optional.empty();
        }
        String nombre = details.getUsername();
        if (details.getUsuario().getEmpleado() != null) {
            var emp = details.getUsuario().getEmpleado();
            nombre = ((emp.getNombre() != null ? emp.getNombre() : "") + " "
                    + (emp.getApellido() != null ? emp.getApellido() : "")).trim();
        }
        return java.util.Optional.of(new UsuarioRef(details.getUsuario().getId(), nombre));
    }

    private record UsuarioRef(Long usuarioId, String nombre) {}
}
