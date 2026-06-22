package com.vida.apirest.controller;

import com.vida.apirest.dto.finanzas.CreateMonedaRequest;
import com.vida.apirest.dto.finanzas.MonedaResponse;
import com.vida.apirest.servicies.MonedaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/monedas")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('VER_ORGANIZACION')")
public class MonedaController {

    private final MonedaService monedaService;

    @PostMapping
    public ResponseEntity<MonedaResponse> createMoneda(@RequestBody CreateMonedaRequest request) {
        return ResponseEntity.ok(monedaService.createMoneda(request));
    }

    @PutMapping("/{codigo}/tasa-cambio")
    public ResponseEntity<MonedaResponse> updateTasaCambio(
            @PathVariable String codigo,
            @RequestParam BigDecimal tasa) {
        return ResponseEntity.ok(monedaService.updateTasaCambio(codigo, tasa));
    }

    @GetMapping
    public ResponseEntity<List<MonedaResponse>> getAllMonedas() {
        return ResponseEntity.ok(monedaService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MonedaResponse> getMonedaById(@PathVariable Long id) {
        return ResponseEntity.ok(monedaService.findById(id));
    }

    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<MonedaResponse> getMonedaByCodigo(@PathVariable String codigo) {
        return ResponseEntity.ok(monedaService.findByCodigo(codigo));
    }

    @GetMapping("/predeterminada")
    public ResponseEntity<MonedaResponse> getMonedaPredeterminada() {
        return ResponseEntity.ok(monedaService.findPredeterminada());
    }
}
