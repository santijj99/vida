package com.vida.apirest.controller;

import com.vida.apirest.dto.afip.AfipAmbienteRequest;
import com.vida.apirest.dto.afip.AfipAmbienteResponse;
import com.vida.apirest.dto.afip.EmitirFacturaAFIPRequest;
import com.vida.apirest.dto.afip.FacturaAFIPResponse;
import com.vida.apirest.dto.afip.ReceptorAfipConsultaResponse;
import com.vida.apirest.dto.afip.TokenValidationResponse;
import com.vida.apirest.model.afip.FacturaAFIP;
import com.vida.apirest.servicies.afip.AFIPTokenValidatorService;
import com.vida.apirest.servicies.afip.AfipConfigService;
import com.vida.apirest.servicies.afip.FacturaAFIPService;
import com.vida.apirest.servicies.afip.ReceptorAfipConsultaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/facturas-afip")
@RequiredArgsConstructor
public class FacturaAFIPController {

    private final FacturaAFIPService facturaAFIPService;
    private final AFIPTokenValidatorService tokenValidatorService;
    private final AfipConfigService afipConfigService;
    private final ReceptorAfipConsultaService receptorAfipConsultaService;

    @GetMapping
    public ResponseEntity<List<FacturaAFIPResponse>> listar() {
        return ResponseEntity.ok(facturaAFIPService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(facturaAFIPService.obtenerDetalle(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "message", e.getMessage(),
                    "statusCode", HttpStatus.NOT_FOUND.value()));
        }
    }

    @GetMapping("/venta/{ventaId}")
    public ResponseEntity<?> obtenerPorVenta(@PathVariable Long ventaId) {
        try {
            return ResponseEntity.ok(facturaAFIPService.obtenerPorVenta(ventaId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "message", e.getMessage(),
                    "statusCode", HttpStatus.NOT_FOUND.value()));
        }
    }

    @PostMapping("/venta/{ventaId}/emitir")
    public ResponseEntity<?> emitir(@PathVariable Long ventaId,
                                    @RequestBody(required = false) EmitirFacturaAFIPRequest request) {
        try {
            FacturaAFIP factura = facturaAFIPService.emitirFactura(ventaId, request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(facturaAFIPService.obtenerDetalle(factura.getIdFacturaAFIP()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", e.getMessage() != null ? e.getMessage() : "Error al emitir factura AFIP",
                    "statusCode", HttpStatus.BAD_REQUEST.value()));
        }
    }

    @GetMapping("/{id}/ticket")
    public ResponseEntity<?> descargarTicket(@PathVariable Long id) {
        try {
            byte[] pdf = facturaAFIPService.generarTicketPdf(id);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "ticket-afip-" + id + ".pdf");
            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", e.getMessage(),
                    "statusCode", HttpStatus.BAD_REQUEST.value()));
        }
    }

    @GetMapping("/token/estado")
    public ResponseEntity<TokenValidationResponse> estadoToken() {
        return ResponseEntity.ok(tokenValidatorService.consultarEstadoToken());
    }

    @PostMapping("/token/validar")
    public ResponseEntity<TokenValidationResponse> validarToken() {
        return ResponseEntity.ok(tokenValidatorService.validarYRegenerarToken());
    }

    @GetMapping("/config/ambiente")
    public ResponseEntity<AfipAmbienteResponse> consultarAmbiente() {
        return ResponseEntity.ok(afipConfigService.consultarAmbiente());
    }

    @PutMapping("/config/ambiente")
    public ResponseEntity<AfipAmbienteResponse> cambiarAmbiente(@RequestBody AfipAmbienteRequest request) {
        if (request.getHomologacion() == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(afipConfigService.cambiarAmbiente(request.getHomologacion()));
    }

    @GetMapping("/receptor/consulta")
    public ResponseEntity<ReceptorAfipConsultaResponse> consultarReceptor(
            @RequestParam Integer docTipo,
            @RequestParam String docNro) {
        return ResponseEntity.ok(receptorAfipConsultaService.consultar(docTipo, docNro));
    }
}
