package com.vida.apirest.controller;

import com.vida.apirest.dto.common.PageResponse;
import com.vida.apirest.dto.venta.AbrirCajaRequest;
import com.vida.apirest.dto.venta.AjusteCajaRequest;
import com.vida.apirest.dto.venta.CajaCuentaResponse;
import com.vida.apirest.dto.venta.CajaMovimientoResponse;
import com.vida.apirest.dto.venta.CajaSesionResponse;
import com.vida.apirest.dto.venta.CerrarCajaRequest;
import com.vida.apirest.servicies.CajaSesionService;
import com.vida.apirest.dto.venta.CreditoSimulacionRequest;
import com.vida.apirest.dto.venta.CreditoSimulacionResponse;
import com.vida.apirest.dto.venta.VentaCancelarRequest;
import com.vida.apirest.dto.venta.VentaCambioArticuloRequest;
import com.vida.apirest.dto.venta.VentaCreateRequest;
import com.vida.apirest.dto.venta.VentaCreditoPersonalRequest;
import com.vida.apirest.dto.venta.VentaHistorialItemResponse;
import com.vida.apirest.dto.venta.VentaResponse;
import com.vida.apirest.dto.empleado.EmpleadoResponse;
import com.vida.apirest.security.Authz;
import com.vida.apirest.servicies.VentaService;
import com.vida.apirest.servicies.EmpleadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/venta")
@RequiredArgsConstructor
public class VentaController {

    private final VentaService ventaService;
    private final CajaSesionService cajaSesionService;
    private final EmpleadoService empleadoService;

    @PostMapping
    @PreAuthorize(Authz.GESTIONAR_VENTAS)
    public ResponseEntity<VentaResponse> registrarVenta(@RequestBody VentaCreateRequest request) {
        try {
            VentaResponse response = ventaService.registrarVenta(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (DataIntegrityViolationException ex) {
            // Carrera: dos POSTs con el mismo clientRequestId; devolver la venta ya committeada.
            if (VentaService.esConflictoClientRequestId(ex)) {
                Optional<VentaResponse> replay =
                        ventaService.buscarRespuestaPorClientRequestId(request.getClientRequestId());
                if (replay.isPresent()) {
                    return ResponseEntity.ok(replay.get());
                }
            }
            throw ex;
        }
    }

    @GetMapping("/vendedores")
    @PreAuthorize(Authz.VER_O_GESTIONAR_VENTAS)
    public ResponseEntity<List<EmpleadoResponse>> listarVendedores() {
        return ResponseEntity.ok(empleadoService.findActivosParaVenta());
    }

    @PostMapping("/credito-personal/simular")
    @PreAuthorize(Authz.VER_O_GESTIONAR_VENTAS)
    public ResponseEntity<CreditoSimulacionResponse> simularCreditoPersonal(@RequestBody CreditoSimulacionRequest request) {
        return ResponseEntity.ok(ventaService.simularCreditoPersonal(request));
    }

    @PostMapping("/credito-personal")
    @PreAuthorize(Authz.GESTIONAR_VENTAS)
    public ResponseEntity<VentaResponse> registrarVentaCreditoPersonal(@RequestBody VentaCreditoPersonalRequest request) {
        VentaResponse response = ventaService.registrarVentaCreditoPersonal(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/historial")
    @PreAuthorize(Authz.VER_O_GESTIONAR_HISTORIAL_VENTAS)
    public ResponseEntity<PageResponse<VentaHistorialItemResponse>> listarHistorial(
            @RequestParam(required = false) Long sucursalId,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean facturadaArca,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        return ResponseEntity.ok(ventaService.listarHistorial(
                sucursalId, estado, desde, hasta, q, facturadaArca, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize(Authz.VER_VENTA_O_HISTORIAL)
    public ResponseEntity<VentaResponse> obtenerVenta(@PathVariable Long id) {
        return ResponseEntity.ok(ventaService.obtenerVenta(id));
    }

    @GetMapping("/{id}/ticket")
    @PreAuthorize(Authz.VER_VENTA_O_HISTORIAL)
    public ResponseEntity<?> descargarTicketVenta(@PathVariable Long id) {
        try {
            byte[] pdf = ventaService.generarTicketVentaPdf(id);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "ticket-venta-" + id + ".pdf");
            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", e.getMessage() != null ? e.getMessage() : "Error al generar ticket",
                    "statusCode", HttpStatus.BAD_REQUEST.value()));
        }
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize(Authz.GESTIONAR_HISTORIAL_VENTAS)
    public ResponseEntity<VentaResponse> cancelarVenta(@PathVariable Long id, @RequestBody VentaCancelarRequest request) {
        return ResponseEntity.ok(ventaService.cancelarVenta(id, request));
    }

    @PostMapping("/{id}/cambio-articulo")
    @PreAuthorize(Authz.GESTIONAR_HISTORIAL_VENTAS)
    public ResponseEntity<VentaResponse> cambiarArticulo(@PathVariable Long id, @RequestBody VentaCambioArticuloRequest request) {
        return ResponseEntity.ok(ventaService.cambiarArticulo(id, request));
    }

    @GetMapping("/caja/cuentas")
    @PreAuthorize(Authz.VER_CAJA_O_VENTAS)
    public ResponseEntity<List<CajaCuentaResponse>> listarCajas() {
        return ResponseEntity.ok(ventaService.listarCajas());
    }

    @GetMapping("/caja/movimientos")
    @PreAuthorize(Authz.VER_O_GESTIONAR_CAJA)
    public ResponseEntity<List<CajaMovimientoResponse>> listarMovimientosCaja(
            @RequestParam(required = false) Long cuentaId) {
        return ResponseEntity.ok(ventaService.listarMovimientosCaja(cuentaId));
    }

    @GetMapping("/caja/sesiones/activa")
    @PreAuthorize(Authz.VER_CAJA_O_VENTAS)
    public ResponseEntity<CajaSesionResponse> sesionActiva(@RequestParam Long cuentaId) {
        CajaSesionResponse sesion = cajaSesionService.obtenerSesionActiva(cuentaId);
        if (sesion == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(sesion);
    }

    @GetMapping("/caja/sesiones")
    @PreAuthorize(Authz.VER_O_GESTIONAR_CAJA)
    public ResponseEntity<List<CajaSesionResponse>> listarSesionesCaja(@RequestParam Long cuentaId) {
        return ResponseEntity.ok(cajaSesionService.listarSesiones(cuentaId));
    }

    @GetMapping("/caja/sesiones/{id}/movimientos")
    @PreAuthorize(Authz.VER_O_GESTIONAR_CAJA)
    public ResponseEntity<List<CajaMovimientoResponse>> listarMovimientosSesion(@PathVariable Long id) {
        return ResponseEntity.ok(cajaSesionService.listarMovimientosSesion(id));
    }

    @PostMapping("/caja/sesiones/abrir")
    @PreAuthorize(Authz.GESTIONAR_CAJA)
    public ResponseEntity<CajaSesionResponse> abrirCaja(@RequestBody AbrirCajaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cajaSesionService.abrirCaja(request));
    }

    @PostMapping("/caja/sesiones/{id}/cerrar")
    @PreAuthorize(Authz.GESTIONAR_CAJA)
    public ResponseEntity<CajaSesionResponse> cerrarCaja(@PathVariable Long id, @RequestBody CerrarCajaRequest request) {
        return ResponseEntity.ok(cajaSesionService.cerrarCaja(id, request));
    }

    @PostMapping("/caja/ajuste")
    @PreAuthorize(Authz.GESTIONAR_CAJA)
    public ResponseEntity<CajaMovimientoResponse> ajustarCaja(@RequestBody AjusteCajaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cajaSesionService.ajustarCaja(request));
    }
}
