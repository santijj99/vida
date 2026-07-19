package com.vida.apirest.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import com.vida.apirest.dto.role.RoleDTO;
import com.vida.apirest.servicies.RoleService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/rol")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMINISTRAR_PERMISOS')")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public ResponseEntity<List<RoleDTO>> getAll() {
        return ResponseEntity.ok(roleService.obtenerTodos());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, String> request) {
        String nombre = request.get("nombre");
        if (nombre == null || nombre.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "El nombre del rol es requerido"));
        }

        RoleDTO roleDTO = roleService.crearRol(nombre);
        return ResponseEntity.status(HttpStatus.CREATED).body(roleDTO);
    }
}
