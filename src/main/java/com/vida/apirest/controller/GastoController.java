package com.vida.apirest.controller;

import com.vida.apirest.dto.finanzas.*;
import com.vida.apirest.servicies.GastoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gastos")
@RequiredArgsConstructor
public class GastoController {

    private final GastoService gastoService;

    @GetMapping
    public ResponseEntity<?> listar(
            @RequestParam(required = false) Long sucursalId,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        try {
            return ResponseEntity.ok(gastoService.listar(sucursalId, estado, categoriaId, q, page, size));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(e, HttpStatus.BAD_REQUEST));
        }
    }

    @GetMapping("/cuentas-pago")
    public ResponseEntity<?> listarCuentasPago(@RequestParam Long sucursalId) {
        try {
            return ResponseEntity.ok(gastoService.listarCuentasPago(sucursalId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(e, HttpStatus.BAD_REQUEST));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(gastoService.findById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(e, HttpStatus.NOT_FOUND));
        }
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody GastoCreateRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(gastoService.crear(request));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(e, HttpStatus.BAD_REQUEST));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody GastoUpdateRequest request) {
        try {
            return ResponseEntity.ok(gastoService.actualizar(id, request));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(e, HttpStatus.BAD_REQUEST));
        }
    }

    @PostMapping("/{id}/aprobar")
    public ResponseEntity<?> aprobar(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(gastoService.aprobar(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(e, HttpStatus.BAD_REQUEST));
        }
    }

    @PostMapping("/{id}/pagos")
    public ResponseEntity<?> registrarPago(@PathVariable Long id, @RequestBody GastoPagoRequest request) {
        try {
            return ResponseEntity.ok(gastoService.registrarPago(id, request));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(e, HttpStatus.BAD_REQUEST));
        }
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<?> cancelar(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(gastoService.cancelar(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(e, HttpStatus.BAD_REQUEST));
        }
    }

    private Map<String, Object> errorBody(RuntimeException e, HttpStatus status) {
        return Map.of("message", e.getMessage(), "statusCode", status.value());
    }
}
