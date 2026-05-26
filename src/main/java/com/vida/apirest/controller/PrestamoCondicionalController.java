package com.vida.apirest.controller;

import com.vida.apirest.dto.prestamo.ConfirmarPrestamoCreditoRequest;
import com.vida.apirest.dto.prestamo.ConfirmarPrestamoRequest;
import com.vida.apirest.dto.prestamo.DevolverPrestamoDetallesRequest;
import com.vida.apirest.dto.prestamo.PrestamoCondicionalCreateRequest;
import com.vida.apirest.dto.prestamo.PrestamoCondicionalResponse;
import com.vida.apirest.servicies.PrestamoCondicionalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/prestamos-condicionales")
@RequiredArgsConstructor
public class PrestamoCondicionalController {

    private final PrestamoCondicionalService prestamoCondicionalService;

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody PrestamoCondicionalCreateRequest request) {
        try {
            PrestamoCondicionalResponse response = prestamoCondicionalService.crear(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<PrestamoCondicionalResponse>> listar(
            @RequestParam(required = false) Long sucursalId,
            @RequestParam(required = false, defaultValue = "ACTIVOS") String estado) {
        return ResponseEntity.ok(prestamoCondicionalService.listar(sucursalId, estado));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(prestamoCondicionalService.obtener(id));
        } catch (RuntimeException e) {
            return error(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @PostMapping("/{id}/devolver")
    public ResponseEntity<?> devolver(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(prestamoCondicionalService.devolver(id));
        } catch (RuntimeException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/{id}/devolver-detalles")
    public ResponseEntity<?> devolverDetalles(
            @PathVariable Long id,
            @RequestBody DevolverPrestamoDetallesRequest request) {
        try {
            return ResponseEntity.ok(prestamoCondicionalService.devolverDetalles(id, request));
        } catch (RuntimeException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/{id}/confirmar")
    public ResponseEntity<?> confirmar(@PathVariable Long id, @RequestBody ConfirmarPrestamoRequest request) {
        try {
            return ResponseEntity.ok(prestamoCondicionalService.confirmarCompra(id, request));
        } catch (RuntimeException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/{id}/confirmar-credito")
    public ResponseEntity<?> confirmarCredito(
            @PathVariable Long id,
            @RequestBody ConfirmarPrestamoCreditoRequest request) {
        try {
            return ResponseEntity.ok(prestamoCondicionalService.confirmarCreditoPersonal(id, request));
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
