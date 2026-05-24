package com.vida.apirest.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vida.apirest.dto.usuario.CreateUsuarioRequest;
import com.vida.apirest.dto.usuario.UpdateUsuarioRequest;
import com.vida.apirest.dto.usuario.UsuarioResponse;
import com.vida.apirest.servicies.UsuarioService;

@RestController
@RequestMapping("/usuario")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> getAll() {
        return ResponseEntity.ok(usuarioService.findAll());
    }

    @PostMapping("/admin/create")
    public ResponseEntity<?> createByAdmin(@RequestBody CreateUsuarioRequest request) {
        try {
            UsuarioResponse response = usuarioService.createByAdmin(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", e.getMessage(),
                    "statusCode", HttpStatus.BAD_REQUEST.value()
            ));
        }
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<?> findById(@PathVariable Long id) {
        try {
            UsuarioResponse response = usuarioService.findById(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "message", e.getMessage(),
                    "statusCode", HttpStatus.NOT_FOUND.value()
            ));
        }
    }

    @PutMapping(value = "/upload/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @ModelAttribute UpdateUsuarioRequest request) {
        try {
            UsuarioResponse response = usuarioService.updateUsuarioConImagen(id, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "message", e.getMessage(),
                    "statusCode", HttpStatus.NOT_FOUND.value()
            ));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "message", e.getMessage(),
                    "statusCode", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
}
