package com.vida.apirest.controller;

import com.vida.apirest.dto.finanzas.CreateMonedaRequest;
import com.vida.apirest.dto.finanzas.MonedaResponse;
import com.vida.apirest.servicies.MonedaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/monedas")
@RequiredArgsConstructor
public class MonedaController {

    private final MonedaService monedaService;

    @PostMapping
    public ResponseEntity<?> createMoneda(@RequestBody CreateMonedaRequest request) {
        try {
            MonedaResponse response = monedaService.createMoneda(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    java.util.Map.of("error", e.getMessage() != null ? e.getMessage() : "Solicitud inválida"));
        }
    }

    @PutMapping("/{codigo}/tasa-cambio")
    public ResponseEntity<?> updateTasaCambio(
            @PathVariable String codigo,
            @RequestParam BigDecimal tasa) {
        try {
            MonedaResponse response = monedaService.updateTasaCambio(codigo, tasa);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    java.util.Map.of("error", e.getMessage() != null ? e.getMessage() : "Solicitud inválida"));
        }
    }

    @GetMapping
    public ResponseEntity<List<MonedaResponse>> getAllMonedas() {
        try {
            List<MonedaResponse> response = monedaService.findAll();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<MonedaResponse> getMonedaById(@PathVariable Long id) {
        try {
            MonedaResponse response = monedaService.findById(id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<MonedaResponse> getMonedaByCodigo(@PathVariable String codigo) {
        try {
            MonedaResponse response = monedaService.findByCodigo(codigo);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/predeterminada")
    public ResponseEntity<MonedaResponse> getMonedaPredeterminada() {
        try {
            MonedaResponse response = monedaService.findPredeterminada();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}