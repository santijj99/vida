package com.vida.apirest.controller;

import com.vida.apirest.dto.auth.PermisoDTO;
import com.vida.apirest.dto.auth.RolePermisosResponse;
import com.vida.apirest.dto.auth.UpdateRolePermisosRequest;
import com.vida.apirest.dto.auth.UpdateUsuarioPermisosRequest;
import com.vida.apirest.dto.auth.UsuarioPermisosResponse;
import com.vida.apirest.servicies.RbacService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
    public ResponseEntity<?> permisosRol(@PathVariable Long roleId) {
        try {
            return ResponseEntity.ok(rbacService.obtenerPermisosRol(roleId));
        } catch (RuntimeException e) {
            return error(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @PutMapping("/roles/{roleId}/permisos")
    @PreAuthorize("hasAuthority('ADMINISTRAR_PERMISOS')")
    public ResponseEntity<?> actualizarPermisosRol(
            @PathVariable Long roleId,
            @RequestBody UpdateRolePermisosRequest request) {
        try {
            RolePermisosResponse response = rbacService.actualizarPermisosRol(roleId, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/usuarios/{usuarioId}/permisos")
    @PreAuthorize("hasAuthority('ADMINISTRAR_PERMISOS')")
    public ResponseEntity<?> permisosUsuario(@PathVariable Long usuarioId) {
        try {
            UsuarioPermisosResponse response = rbacService.obtenerPermisosUsuario(usuarioId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return error(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @PutMapping("/usuarios/{usuarioId}/permisos")
    @PreAuthorize("hasAuthority('ADMINISTRAR_PERMISOS')")
    public ResponseEntity<?> actualizarPermisosUsuario(
            @PathVariable Long usuarioId,
            @RequestBody UpdateUsuarioPermisosRequest request) {
        try {
            UsuarioPermisosResponse response = rbacService.actualizarPermisosUsuario(usuarioId, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "message", message != null ? message : "Error",
                "statusCode", status.value()));
    }
}
