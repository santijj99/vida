package com.vida.apirest.controller;

import com.vida.apirest.dto.credito.AnularPagoCuotaRequest;
import com.vida.apirest.dto.credito.ClienteCreditosResponse;
import com.vida.apirest.dto.credito.CuentaCreditoListResponse;
import com.vida.apirest.dto.credito.PagoCuotaResponse;
import com.vida.apirest.dto.credito.PagoCuotasRequest;
import com.vida.apirest.dto.credito.PagoCuotasResponse;
import com.vida.apirest.dto.credito.TicketPagoCuotasRequest;
import com.vida.apirest.dto.common.PageResponse;
import com.vida.apirest.servicies.CreditoCuentaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    @GetMapping("/ticket-pago")
    public ResponseEntity<?> descargarTicketPagoGet(@RequestParam List<Long> pagoIds) {
        return descargarTicketPago(pagoIds);
    }

    @PostMapping("/ticket-pago")
    public ResponseEntity<?> descargarTicketPagoPost(@RequestBody TicketPagoCuotasRequest request) {
        List<Long> pagoIds = request != null ? request.getPagoIds() : List.of();
        return descargarTicketPago(pagoIds);
    }

    private ResponseEntity<?> descargarTicketPago(List<Long> pagoIds) {
        try {
            byte[] pdf = creditoCuentaService.generarTicketPagoCuotasPdf(pagoIds);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "ticket-cuota-" + pagoIds.get(0) + ".pdf");
            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", e.getMessage() != null ? e.getMessage() : "Error al generar ticket",
                    "statusCode", HttpStatus.BAD_REQUEST.value()));
        }
    }

    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<ClienteCreditosResponse> creditosPorCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(creditoCuentaService.obtenerCreditosPorCliente(clienteId));
    }

    @GetMapping("/{id}/pagos")
    public ResponseEntity<List<PagoCuotaResponse>> listarPagos(@PathVariable Long id) {
        return ResponseEntity.ok(creditoCuentaService.listarPagosPorCuentaConBackfill(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteCreditosResponse> detalleCreditos(@PathVariable Long id) {
        return ResponseEntity.ok(creditoCuentaService.obtenerCreditosPorCuenta(id));
    }

    @GetMapping("/{id}/resumen")
    public ResponseEntity<?> descargarResumenCuenta(@PathVariable Long id) {
        try {
            byte[] pdf = creditoCuentaService.generarResumenCuentaPdf(id);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "resumen-cuenta-" + id + ".pdf");
            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", e.getMessage() != null ? e.getMessage() : "Error al generar resumen",
                    "statusCode", HttpStatus.BAD_REQUEST.value()));
        }
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
