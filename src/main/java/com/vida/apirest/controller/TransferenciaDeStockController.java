package com.vida.apirest.controller;

import com.vida.apirest.dto.almacen.StockDepositoResponse;
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

@RestController
@RequestMapping("/api/transferencias-stock")
@RequiredArgsConstructor
public class TransferenciaDeStockController {

    private final TransferenciaDeStockService transferenciaDeStockService;

    @GetMapping("/stock-disponible")
    @PreAuthorize("hasAuthority('LEER_STOCK')")
    public ResponseEntity<PageResponse<StockDepositoResponse>> listarStockDeposito(
            @RequestParam Long depositoId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        return ResponseEntity.ok(transferenciaDeStockService.listarStockDeposito(depositoId, q, page, size));
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
    public ResponseEntity<TransferenciaStockResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(transferenciaDeStockService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('TRANSFERIR_STOCK')")
    public ResponseEntity<TransferenciaStockResponse> crear(@RequestBody TransferenciaStockCreateRequest request) {
        TransferenciaStockResponse response = transferenciaDeStockService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
