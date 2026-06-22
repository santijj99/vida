package com.vida.apirest.controller;

import com.vida.apirest.dto.auth.PermisoDTO;
import com.vida.apirest.dto.auth.RolePermisosResponse;
import com.vida.apirest.dto.auth.UpdateRolePermisosRequest;
import com.vida.apirest.dto.auth.UpdateUsuarioPermisosRequest;
import com.vida.apirest.dto.auth.UsuarioPermisosResponse;
import com.vida.apirest.servicies.RbacService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rbac")
@RequiredArgsConstructor
public class RbacController {

    private final RbacService rbacService;

    @GetMapping("/permisos")
    @PreAuthorize("hasAuthority('ADMINISTRAR_PERMISOS')")
    public ResponseEntity<List<PermisoDTO>> listarPermisos() {
        return ResponseEntity.ok(rbacService.listarPermisos());
    }

    @GetMapping("/roles/{roleId}/permisos")
    @PreAuthorize("hasAuthority('ADMINISTRAR_PERMISOS')")
    public ResponseEntity<RolePermisosResponse> permisosRol(@PathVariable Long roleId) {
        return ResponseEntity.ok(rbacService.obtenerPermisosRol(roleId));
    }

    @PutMapping("/roles/{roleId}/permisos")
    @PreAuthorize("hasAuthority('ADMINISTRAR_PERMISOS')")
    public ResponseEntity<RolePermisosResponse> actualizarPermisosRol(
            @PathVariable Long roleId,
            @RequestBody UpdateRolePermisosRequest request) {
        return ResponseEntity.ok(rbacService.actualizarPermisosRol(roleId, request));
    }

    @GetMapping("/usuarios/{usuarioId}/permisos")
    @PreAuthorize("hasAuthority('ADMINISTRAR_PERMISOS')")
    public ResponseEntity<UsuarioPermisosResponse> permisosUsuario(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(rbacService.obtenerPermisosUsuario(usuarioId));
    }

    @PutMapping("/usuarios/{usuarioId}/permisos")
    @PreAuthorize("hasAuthority('ADMINISTRAR_PERMISOS')")
    public ResponseEntity<UsuarioPermisosResponse> actualizarPermisosUsuario(
            @PathVariable Long usuarioId,
            @RequestBody UpdateUsuarioPermisosRequest request) {
        return ResponseEntity.ok(rbacService.actualizarPermisosUsuario(usuarioId, request));
    }
}
