package com.vida.apirest.controller;

import com.vida.apirest.dto.common.PageResponse;
import com.vida.apirest.dto.proveedor.ProveedorRequest;
import com.vida.apirest.dto.proveedor.ProveedorResponse;
import com.vida.apirest.servicies.ProveedorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/proveedores")
@RequiredArgsConstructor
public class ProveedorController {

    private final ProveedorService proveedorService;

    @GetMapping
    public ResponseEntity<List<ProveedorResponse>> listarActivos() {
        return ResponseEntity.ok(proveedorService.findAllActivos());
    }

    @GetMapping("/pagina")
    public ResponseEntity<PageResponse<ProveedorResponse>> pagina(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "true") boolean soloActivos) {
        return ResponseEntity.ok(proveedorService.findPage(q, page, size, soloActivos));
    }

    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<?> obtener(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(proveedorService.findById(id));
        } catch (RuntimeException e) {
            return error(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody ProveedorRequest request) {
        try {
            ProveedorResponse response = proveedorService.create(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PutMapping("/{id:[0-9]+}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody ProveedorRequest request) {
        try {
            return ResponseEntity.ok(proveedorService.update(id, request));
        } catch (RuntimeException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @DeleteMapping("/{id:[0-9]+}")
    public ResponseEntity<?> desactivar(@PathVariable Long id) {
        try {
            proveedorService.desactivar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return error(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "message", message != null ? message : "Error",
                "statusCode", status.value()));
    }
}
