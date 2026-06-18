package com.vida.apirest.controller;

import com.vida.apirest.dto.finanzas.GastoCategoriaRequest;
import com.vida.apirest.dto.finanzas.GastoCategoriaResponse;
import com.vida.apirest.servicies.GastoCategoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gastos-categorias")
@RequiredArgsConstructor
public class GastoCategoriaController {

    private final GastoCategoriaService gastoCategoriaService;

    @GetMapping
    public ResponseEntity<List<GastoCategoriaResponse>> listar(
            @RequestParam(required = false, defaultValue = "true") Boolean soloActivas) {
        return ResponseEntity.ok(gastoCategoriaService.listar(soloActivas));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(gastoCategoriaService.findById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(e, HttpStatus.NOT_FOUND));
        }
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody GastoCategoriaRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(gastoCategoriaService.crear(request));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(e, HttpStatus.BAD_REQUEST));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody GastoCategoriaRequest request) {
        try {
            return ResponseEntity.ok(gastoCategoriaService.actualizar(id, request));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(e, HttpStatus.BAD_REQUEST));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> desactivar(@PathVariable Long id) {
        try {
            gastoCategoriaService.desactivar(id);
            return ResponseEntity.ok(Map.of("message", "Categoría desactivada"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(e, HttpStatus.BAD_REQUEST));
        }
    }

    private Map<String, Object> errorBody(RuntimeException e, HttpStatus status) {
        return Map.of("message", e.getMessage(), "statusCode", status.value());
    }
}
