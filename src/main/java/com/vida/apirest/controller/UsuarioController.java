package com.vida.apirest.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vida.apirest.dto.usuario.CreateUsuarioRequest;
import com.vida.apirest.dto.usuario.AdminUpdateUsuarioRequest;
import com.vida.apirest.dto.usuario.UpdateUsuarioRequest;
import com.vida.apirest.dto.usuario.UsuarioResponse;
import com.vida.apirest.servicies.UsuarioService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/usuario")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    @PreAuthorize("hasAuthority('LEER_USUARIOS')")
    public ResponseEntity<List<UsuarioResponse>> getAll() {
        return ResponseEntity.ok(usuarioService.findAll());
    }

    @PostMapping("/admin/create")
    @PreAuthorize("hasAuthority('CREAR_USUARIOS')")
    public ResponseEntity<UsuarioResponse> createByAdmin(@RequestBody CreateUsuarioRequest request) {
        UsuarioResponse response = usuarioService.createByAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/admin/{id}")
    @PreAuthorize("hasAuthority('MODIFICAR_USUARIOS')")
    public ResponseEntity<UsuarioResponse> updateByAdmin(
            @PathVariable Long id,
            @RequestBody AdminUpdateUsuarioRequest request) {
        return ResponseEntity.ok(usuarioService.updateByAdmin(id, request));
    }

    @GetMapping(value = "/{id}")
    @PreAuthorize("hasAuthority('LEER_USUARIOS')")
    public ResponseEntity<UsuarioResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.findById(id));
    }

    @PutMapping(value = "/upload/{id}")
    @PreAuthorize("hasAuthority('MODIFICAR_USUARIOS')")
    public ResponseEntity<UsuarioResponse> update(@PathVariable Long id, @ModelAttribute UpdateUsuarioRequest request) throws IOException {
        return ResponseEntity.ok(usuarioService.updateUsuarioConImagen(id, request));
    }

    @PostMapping("/{usuarioId}/asignar-rol/{rolId}")
    @PreAuthorize("hasAuthority('MODIFICAR_USUARIOS')")
    public ResponseEntity<UsuarioResponse> asignarRol(@PathVariable Long usuarioId, @PathVariable Long rolId) {
        return ResponseEntity.ok(usuarioService.asignarRol(usuarioId, rolId));
    }
}
