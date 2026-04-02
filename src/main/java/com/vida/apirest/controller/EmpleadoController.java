package com.vida.apirest.controller;

import com.vida.apirest.dto.empleado.CreateEmpleadoRequest;
import com.vida.apirest.dto.empleado.EmpleadoResponse;
import com.vida.apirest.servicies.EmpleadoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/empleado")
public class EmpleadoController {

    @Autowired
    private EmpleadoService empleadoService;

    @GetMapping
    public ResponseEntity<List<EmpleadoResponse>> getAll() {
        return ResponseEntity.ok(empleadoService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            EmpleadoResponse response = empleadoService.findById(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage(), "statusCode", HttpStatus.NOT_FOUND.value()));
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateEmpleadoRequest request) {
        try {
            EmpleadoResponse response = empleadoService.create(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage(), "statusCode", HttpStatus.BAD_REQUEST.value()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody CreateEmpleadoRequest request) {
        try {
            EmpleadoResponse response = empleadoService.update(id, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage(), "statusCode", HttpStatus.NOT_FOUND.value()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            empleadoService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage(), "statusCode", HttpStatus.NOT_FOUND.value()));
        }
    }
}
