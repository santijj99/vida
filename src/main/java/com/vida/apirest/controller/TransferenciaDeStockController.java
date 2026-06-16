package com.vida.apirest.controller;

import com.vida.apirest.dto.almacen.TransferenciaStockCreateRequest;
import com.vida.apirest.dto.almacen.TransferenciaStockResponse;
import com.vida.apirest.dto.common.PageResponse;
import com.vida.apirest.servicies.TransferenciaDeStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transferencias-stock")
@RequiredArgsConstructor
public class TransferenciaDeStockController {

    private final TransferenciaDeStockService transferenciaDeStockService;

    @GetMapping("/stock-disponible")
    @PreAuthorize("hasAuthority('LEER_STOCK')")
    public ResponseEntity<?> listarStockDeposito(
            @RequestParam Long depositoId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        try {
            PageResponse<?> response = transferenciaDeStockService.listarStockDeposito(depositoId, q, page, size);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('LEER_STOCK')")
    public ResponseEntity<List<TransferenciaStockResponse>> listar(
            @RequestParam(required = false) Long depositoOrigenId,
            @RequestParam(required = false) Long sucursalDestinoId) {
        return ResponseEntity.ok(transferenciaDeStockService.listar(depositoOrigenId, sucursalDestinoId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('LEER_STOCK')")
    public ResponseEntity<?> obtener(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(transferenciaDeStockService.obtener(id));
        } catch (RuntimeException e) {
            return error(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('TRANSFERIR_STOCK')")
    public ResponseEntity<?> crear(@RequestBody TransferenciaStockCreateRequest request) {
        try {
            TransferenciaStockResponse response = transferenciaDeStockService.crear(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
