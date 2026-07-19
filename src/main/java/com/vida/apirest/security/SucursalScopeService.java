package com.vida.apirest.security;

import com.vida.apirest.exception.ForbiddenException;
import com.vida.apirest.model.persona.Empleado;
import com.vida.apirest.repositories.EmpleadoRepository;
import com.vida.apirest.repositories.FinanzasCuentaFinancieraRepository;
import com.vida.apirest.repositories.UsuarioSucursalRepository;
import com.vida.apirest.repositories.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Limita operaciones por sucursal para mitigar IDOR horizontal.
 * ADMINISTRADOR: acceso global. Resto: sucursales en usuario_sucursal + cuentas/ventas del empleado.
 */
@Service
@RequiredArgsConstructor
public class SucursalScopeService {

    private static final String ROLE_ADMINISTRADOR = "ROLE_ADMINISTRADOR";

    private final EmpleadoRepository empleadoRepository;
    private final FinanzasCuentaFinancieraRepository cuentaFinancieraRepository;
    private final VentaRepository ventaRepository;
    private final UsuarioSucursalRepository usuarioSucursalRepository;

    public boolean hasGlobalAccess() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> ROLE_ADMINISTRADOR.equals(a.getAuthority()));
    }

    /**
     * Valida y devuelve el filtro de sucursal para listados.
     * Usuarios no admin no pueden omitir sucursalId si tienen más de una sucursal asignada.
     */
    public Long enforceFilter(Long requestedSucursalId) {
        if (hasGlobalAccess()) {
            return requestedSucursalId;
        }

        Set<Long> allowed = allowedSucursalIds();
        if (allowed.isEmpty()) {
            throw new ForbiddenException("No tiene sucursales asignadas");
        }

        if (requestedSucursalId != null) {
            if (!allowed.contains(requestedSucursalId)) {
                throw new ForbiddenException("No tiene acceso a la sucursal solicitada");
            }
            return requestedSucursalId;
        }

        if (allowed.size() == 1) {
            return allowed.iterator().next();
        }

        throw new ForbiddenException("Debe indicar sucursalId");
    }

    /** Valida acceso a un recurso ya persistido (GET/PUT/DELETE por id). */
    public void assertCanAccess(Long sucursalId) {
        if (sucursalId == null) {
            throw new ForbiddenException("Recurso sin sucursal");
        }
        if (hasGlobalAccess()) {
            return;
        }
        if (!allowedSucursalIds().contains(sucursalId)) {
            throw new ForbiddenException("No tiene acceso a este recurso");
        }
    }

    /** Valida sucursal en altas/operaciones (body o path). */
    public void assertCanUse(Long sucursalId) {
        assertCanAccess(sucursalId);
    }

    @Transactional(readOnly = true)
    public Set<Long> allowedSucursalIds() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUserDetails details)) {
            return Set.of();
        }

        Long usuarioId = details.getUsuario().getId();
        Set<Long> ids = new LinkedHashSet<>(usuarioSucursalRepository.findSucursalIdsByUsuarioId(usuarioId));

        Long empleadoId = empleadoRepository.findByUsuario_Id(usuarioId)
                .map(Empleado::getId)
                .orElse(null);
        if (empleadoId != null) {
            ids.addAll(cuentaFinancieraRepository.findDistinctSucursalIdsByEmpleadoResponsableId(empleadoId));
            ids.addAll(ventaRepository.findDistinctSucursalIdsByEmpleadoId(empleadoId));
        }

        return Set.copyOf(ids);
    }
}
