package com.vida.apirest.controller;

import com.vida.apirest.dto.common.PageResponse;
import com.vida.apirest.dto.finanzas.*;
import com.vida.apirest.servicies.GastoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gastos")
@RequiredArgsConstructor
public class GastoController {

    private final GastoService gastoService;

    @GetMapping
    @PreAuthorize("hasAuthority('VER_GASTOS')")
    public ResponseEntity<PageResponse<GastoResponse>> listar(
            @RequestParam(required = false) Long sucursalId,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        return ResponseEntity.ok(gastoService.listar(sucursalId, estado, categoriaId, q, page, size));
    }

    @GetMapping("/cuentas-pago")
    @PreAuthorize("hasAuthority('VER_GASTOS')")
    public ResponseEntity<List<CuentaFinancieraResponse>> listarCuentasPago(@RequestParam Long sucursalId) {
        return ResponseEntity.ok(gastoService.listarCuentasPago(sucursalId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VER_GASTOS')")
    public ResponseEntity<GastoResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(gastoService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('GESTIONAR_GASTOS')")
    public ResponseEntity<GastoResponse> crear(@RequestBody GastoCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gastoService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('GESTIONAR_GASTOS')")
    public ResponseEntity<GastoResponse> actualizar(@PathVariable Long id, @RequestBody GastoUpdateRequest request) {
        return ResponseEntity.ok(gastoService.actualizar(id, request));
    }

    @PostMapping("/{id}/aprobar")
    @PreAuthorize("hasAuthority('GESTIONAR_GASTOS')")
    public ResponseEntity<GastoResponse> aprobar(@PathVariable Long id) {
        return ResponseEntity.ok(gastoService.aprobar(id));
    }

    @PostMapping("/{id}/pagos")
    @PreAuthorize("hasAuthority('GESTIONAR_GASTOS')")
    public ResponseEntity<GastoResponse> registrarPago(@PathVariable Long id, @RequestBody GastoPagoRequest request) {
        return ResponseEntity.ok(gastoService.registrarPago(id, request));
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasAuthority('GESTIONAR_GASTOS')")
    public ResponseEntity<GastoResponse> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(gastoService.cancelar(id));
    }
}
