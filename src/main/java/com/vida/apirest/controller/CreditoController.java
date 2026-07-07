package com.vida.apirest.controller;

import com.vida.apirest.dto.credito.CreditoClienteResponse;
import com.vida.apirest.dto.credito.CreditoHistorialResponse;
import com.vida.apirest.dto.credito.EditarCreditoRequest;
import com.vida.apirest.servicies.CreditoHistorialService;
import com.vida.apirest.servicies.CreditoRefinanciacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/creditos")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('VER_CUENTAS')")
public class CreditoController {

    private final CreditoRefinanciacionService refinanciacionService;
    private final CreditoHistorialService historialService;

    @PutMapping("/{creditoId}")
    @PreAuthorize("hasAuthority('EDITAR_CREDITOS')")
    public ResponseEntity<CreditoClienteResponse> editar(
            @PathVariable Long creditoId,
            @RequestBody EditarCreditoRequest request) {
        return ResponseEntity.ok(refinanciacionService.editarCredito(creditoId, request));
    }

    @PostMapping("/cuotas/{cuotaId}/quitar-recargo")
    @PreAuthorize("hasAuthority('EDITAR_CREDITOS')")
    public ResponseEntity<Map<String, String>> quitarRecargo(@PathVariable Long cuotaId) {
        refinanciacionService.quitarRecargoCuota(cuotaId);
        return ResponseEntity.ok(Map.of("message", "Recargo eliminado"));
    }

    @GetMapping("/{creditoId}/historial")
    public ResponseEntity<List<CreditoHistorialResponse>> historial(@PathVariable Long creditoId) {
        return ResponseEntity.ok(historialService.listarPorCredito(creditoId));
    }
}
