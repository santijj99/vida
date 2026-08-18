package com.vida.apirest.controller;

import com.vida.apirest.dto.credito.CancelarCreditoRequest;
import com.vida.apirest.dto.credito.CreditoClienteResponse;
import com.vida.apirest.dto.credito.CreditoHistorialResponse;
import com.vida.apirest.dto.credito.EditarCreditoRequest;
import com.vida.apirest.dto.credito.QuitarRecargoRequest;
import com.vida.apirest.security.Authz;
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
@PreAuthorize(Authz.VER_O_GESTIONAR_CUENTAS)
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
    public ResponseEntity<Map<String, String>> quitarRecargo(
            @PathVariable Long cuotaId,
            @RequestBody(required = false) QuitarRecargoRequest request) {
        String motivo = request != null ? request.getMotivo() : null;
        refinanciacionService.quitarRecargoCuota(cuotaId, motivo);
        return ResponseEntity.ok(Map.of("message", "Recargo eliminado"));
    }

    @PostMapping("/{creditoId}/cancelar")
    @PreAuthorize("hasAuthority('CANCELAR_CREDITOS')")
    public ResponseEntity<CreditoClienteResponse> cancelar(
            @PathVariable Long creditoId,
            @RequestBody CancelarCreditoRequest request) {
        return ResponseEntity.ok(refinanciacionService.cancelarCredito(
                creditoId,
                request != null ? request.getMotivo() : null));
    }

    @GetMapping("/{creditoId}/historial")
    public ResponseEntity<List<CreditoHistorialResponse>> historial(@PathVariable Long creditoId) {
        return ResponseEntity.ok(historialService.listarPorCredito(creditoId));
    }
}
