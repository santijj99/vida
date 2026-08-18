package com.vida.apirest.controller;

import com.vida.apirest.dto.finanzas.CreateTipoCambioRequest;
import com.vida.apirest.dto.finanzas.TipoCambioResponse;
import com.vida.apirest.security.Authz;
import com.vida.apirest.servicies.TipoCambioService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/tipo-cambio")
@RequiredArgsConstructor
@PreAuthorize(Authz.VER_O_GESTIONAR_ORGANIZACION)
public class TipoCambioController {

    private final TipoCambioService tipoCambioService;

    @PostMapping
    @PreAuthorize(Authz.GESTIONAR_ORGANIZACION)
    public ResponseEntity<TipoCambioResponse> createTipoCambio(@RequestBody CreateTipoCambioRequest request) {
        return ResponseEntity.ok(tipoCambioService.createTipoCambio(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Authz.GESTIONAR_ORGANIZACION)
    public ResponseEntity<TipoCambioResponse> updateTipoCambio(
            @PathVariable Long id,
            @RequestBody CreateTipoCambioRequest request) {
        return ResponseEntity.ok(tipoCambioService.updateTipoCambio(id, request));
    }

    @GetMapping
    public ResponseEntity<List<TipoCambioResponse>> getAllTipoCambio() {
        return ResponseEntity.ok(tipoCambioService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipoCambioResponse> getTipoCambioById(@PathVariable Long id) {
        return ResponseEntity.ok(tipoCambioService.findById(id));
    }

    @GetMapping("/moneda/{monedaId}")
    public ResponseEntity<List<TipoCambioResponse>> getTipoCambioByMoneda(@PathVariable Long monedaId) {
        return ResponseEntity.ok(tipoCambioService.findByMoneda(monedaId));
    }

    @GetMapping("/moneda/{monedaId}/fecha/{fecha}")
    public ResponseEntity<TipoCambioResponse> getTipoCambioByMonedaAndFecha(
            @PathVariable Long monedaId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(tipoCambioService.findByMonedaAndFecha(monedaId, fecha));
    }

    @GetMapping("/fecha/{fecha}")
    public ResponseEntity<List<TipoCambioResponse>> getTipoCambioByFecha(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(tipoCambioService.findByFecha(fecha));
    }

    @GetMapping("/moneda/{monedaId}/ultimo")
    public ResponseEntity<TipoCambioResponse> getUltimoTipoCambio(@PathVariable Long monedaId) {
        return ResponseEntity.ok(tipoCambioService.findUltimoTipoCambio(monedaId));
    }
}
