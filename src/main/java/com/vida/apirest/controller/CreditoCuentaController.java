package com.vida.apirest.controller;

import com.vida.apirest.dto.credito.AnularPagoCuotaRequest;
import com.vida.apirest.dto.credito.ClienteCreditosResponse;
import com.vida.apirest.dto.credito.CuentaCreditoListResponse;
import com.vida.apirest.dto.credito.PagoCuotaResponse;
import com.vida.apirest.dto.credito.PagoCuotasRequest;
import com.vida.apirest.dto.credito.PagoCuotasResponse;
import com.vida.apirest.dto.common.PageResponse;
import com.vida.apirest.servicies.CreditoCuentaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cuentas-credito")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('VER_CUENTAS')")
public class CreditoCuentaController {

    private final CreditoCuentaService creditoCuentaService;

    @GetMapping
    public ResponseEntity<List<CuentaCreditoListResponse>> listar(
            @RequestParam(required = false) Long sucursalId) {
        return ResponseEntity.ok(creditoCuentaService.listarCuentas(sucursalId));
    }

    @GetMapping("/pagina")
    public ResponseEntity<PageResponse<CuentaCreditoListResponse>> listarPagina(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long sucursalId,
            @RequestParam(required = false) String estadoCredito) {
        return ResponseEntity.ok(creditoCuentaService.listarCuentasPage(sucursalId, q, estadoCredito, page, size));
    }

    @GetMapping("/{id}/pagos")
    public ResponseEntity<List<PagoCuotaResponse>> listarPagos(@PathVariable Long id) {
        return ResponseEntity.ok(creditoCuentaService.listarPagosPorCuentaConBackfill(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteCreditosResponse> detalleCreditos(@PathVariable Long id) {
        return ResponseEntity.ok(creditoCuentaService.obtenerCreditosPorCuenta(id));
    }

    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<ClienteCreditosResponse> creditosPorCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(creditoCuentaService.obtenerCreditosPorCliente(clienteId));
    }

    @PostMapping("/pagar-cuotas")
    public ResponseEntity<PagoCuotasResponse> pagarCuotas(@RequestBody PagoCuotasRequest request) {
        return ResponseEntity.ok(creditoCuentaService.pagarCuotas(request));
    }

    @PostMapping("/pagos/{pagoId}/anular")
    public ResponseEntity<PagoCuotaResponse> anularPago(
            @PathVariable Long pagoId,
            @RequestBody(required = false) AnularPagoCuotaRequest request) {
        String motivo = request != null ? request.getMotivo() : null;
        return ResponseEntity.ok(creditoCuentaService.anularPago(pagoId, motivo));
    }
}
