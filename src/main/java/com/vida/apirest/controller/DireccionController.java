package com.vida.apirest.controller;

import com.vida.apirest.dto.cliente.DireccionRequest;
import com.vida.apirest.dto.cliente.DireccionResponse;
import com.vida.apirest.servicies.DireccionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/direccion")
public class DireccionController {

    @Autowired
    private DireccionService direccionService;

    @GetMapping
    public ResponseEntity<List<DireccionResponse>> getAll() {
        List<DireccionResponse> direcciones = direccionService.findAll();
        return ResponseEntity.ok(direcciones);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            DireccionResponse response = direccionService.findById(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage(), "statusCode", HttpStatus.NOT_FOUND.value()));
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody DireccionRequest request) {
        try {
            DireccionResponse response = direccionService.create(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage(), "statusCode", HttpStatus.BAD_REQUEST.value()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody DireccionRequest request) {
        try {
            DireccionResponse response = direccionService.update(id, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage(), "statusCode", HttpStatus.NOT_FOUND.value()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            direccionService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage(), "statusCode", HttpStatus.NOT_FOUND.value()));
        }
    }
}
