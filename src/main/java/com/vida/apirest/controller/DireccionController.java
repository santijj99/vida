package com.vida.apirest.controller;

import com.vida.apirest.dto.cliente.DireccionRequest;
import com.vida.apirest.dto.cliente.DireccionResponse;
import com.vida.apirest.security.Authz;
import com.vida.apirest.servicies.DireccionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/direccion")
@RequiredArgsConstructor
@PreAuthorize(Authz.VER_O_GESTIONAR_CLIENTES)
public class DireccionController {

    private final DireccionService direccionService;

    @GetMapping
    public ResponseEntity<List<DireccionResponse>> getAll() {
        return ResponseEntity.ok(direccionService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DireccionResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(direccionService.findById(id));
    }

    @PostMapping
    @PreAuthorize(Authz.GESTIONAR_CLIENTES)
    public ResponseEntity<DireccionResponse> create(@RequestBody DireccionRequest request) {
        DireccionResponse response = direccionService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Authz.GESTIONAR_CLIENTES)
    public ResponseEntity<DireccionResponse> update(@PathVariable Long id, @RequestBody DireccionRequest request) {
        return ResponseEntity.ok(direccionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Authz.GESTIONAR_CLIENTES)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        direccionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
