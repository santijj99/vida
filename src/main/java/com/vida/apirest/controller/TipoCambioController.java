package com.vida.apirest.controller;

import com.vida.apirest.dto.finanzas.CreateTipoCambioRequest;
import com.vida.apirest.dto.finanzas.TipoCambioResponse;
import com.vida.apirest.servicies.TipoCambioService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/tipo-cambio")
@RequiredArgsConstructor
public class TipoCambioController {

    private final TipoCambioService tipoCambioService;

    @PostMapping
    public ResponseEntity<TipoCambioResponse> createTipoCambio(@RequestBody CreateTipoCambioRequest request) {
        try {
            TipoCambioResponse response = tipoCambioService.createTipoCambio(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<TipoCambioResponse> updateTipoCambio(
            @PathVariable Long id,
            @RequestBody CreateTipoCambioRequest request) {
        try {
            TipoCambioResponse response = tipoCambioService.updateTipoCambio(id, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<TipoCambioResponse>> getAllTipoCambio() {
        try {
            List<TipoCambioResponse> response = tipoCambioService.findAll();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipoCambioResponse> getTipoCambioById(@PathVariable Long id) {
        try {
            TipoCambioResponse response = tipoCambioService.findById(id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/moneda/{monedaId}")
    public ResponseEntity<List<TipoCambioResponse>> getTipoCambioByMoneda(@PathVariable Long monedaId) {
        try {
            List<TipoCambioResponse> response = tipoCambioService.findByMoneda(monedaId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/moneda/{monedaId}/fecha/{fecha}")
    public ResponseEntity<TipoCambioResponse> getTipoCambioByMonedaAndFecha(
            @PathVariable Long monedaId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        try {
            TipoCambioResponse response = tipoCambioService.findByMonedaAndFecha(monedaId, fecha);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/fecha/{fecha}")
    public ResponseEntity<List<TipoCambioResponse>> getTipoCambioByFecha(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        try {
            List<TipoCambioResponse> response = tipoCambioService.findByFecha(fecha);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/moneda/{monedaId}/ultimo")
    public ResponseEntity<TipoCambioResponse> getUltimoTipoCambio(@PathVariable Long monedaId) {
        try {
            TipoCambioResponse response = tipoCambioService.findUltimoTipoCambio(monedaId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}