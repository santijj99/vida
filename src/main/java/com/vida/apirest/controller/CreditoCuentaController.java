package com.vida.apirest.controller;

import com.vida.apirest.dto.credito.ClienteCreditosResponse;
import com.vida.apirest.dto.credito.CuentaCreditoListResponse;
import com.vida.apirest.dto.credito.PagoCuotasRequest;
import com.vida.apirest.dto.credito.PagoCuotasResponse;
import com.vida.apirest.dto.common.PageResponse;
import com.vida.apirest.servicies.CreditoCuentaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cuentas-credito")
@RequiredArgsConstructor
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
            @RequestParam(required = false) Long sucursalId) {
        return ResponseEntity.ok(creditoCuentaService.listarCuentasPage(sucursalId, q, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> detalleCreditos(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(creditoCuentaService.obtenerCreditosPorCuenta(id));
        } catch (RuntimeException e) {
            return error(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<?> creditosPorCliente(@PathVariable Long clienteId) {
        try {
            return ResponseEntity.ok(creditoCuentaService.obtenerCreditosPorCliente(clienteId));
        } catch (RuntimeException e) {
            return error(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @PostMapping("/pagar-cuotas")
    public ResponseEntity<?> pagarCuotas(@RequestBody PagoCuotasRequest request) {
        try {
            PagoCuotasResponse response = creditoCuentaService.pagarCuotas(request);
            return ResponseEntity.ok(response);
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
