package com.vida.apirest.controller;

import com.vida.apirest.dto.carrito.*;
import com.vida.apirest.servicies.CarritoPendienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/carritos-pendientes")
@RequiredArgsConstructor
public class CarritoPendienteController {

    private final CarritoPendienteService carritoPendienteService;

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody CarritoPendienteCreateRequest request) {
        try {
            CarritoPendienteResponse response = carritoPendienteService.crear(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<CarritoPendienteResponse>> listar(
            @RequestParam(required = false) Long sucursalId,
            @RequestParam(required = false, defaultValue = "PENDIENTES") String estado) {
        return ResponseEntity.ok(carritoPendienteService.listar(sucursalId, estado));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(carritoPendienteService.obtener(id));
        } catch (RuntimeException e) {
            return error(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<?> cancelar(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(carritoPendienteService.cancelar(id));
        } catch (RuntimeException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/{id}/confirmar")
    public ResponseEntity<?> confirmar(@PathVariable Long id, @RequestBody ConfirmarCarritoPendienteRequest request) {
        try {
            return ResponseEntity.ok(carritoPendienteService.confirmar(id, request));
        } catch (RuntimeException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/{id}/confirmar-credito")
    public ResponseEntity<?> confirmarCredito(
            @PathVariable Long id,
            @RequestBody ConfirmarCarritoPendienteCreditoRequest request) {
        try {
            return ResponseEntity.ok(carritoPendienteService.confirmarCredito(id, request));
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
