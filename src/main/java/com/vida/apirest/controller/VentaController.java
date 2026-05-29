package com.vida.apirest.controller;

import com.vida.apirest.dto.common.PageResponse;
import com.vida.apirest.dto.venta.AbrirCajaRequest;
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
import com.vida.apirest.servicies.VentaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/venta")
public class VentaController {

    private static final Logger logger = LoggerFactory.getLogger(VentaController.class);

    @Autowired
    private VentaService ventaService;

    @Autowired
    private CajaSesionService cajaSesionService;

    @PostMapping
    public ResponseEntity<?> registrarVenta(@RequestBody VentaCreateRequest request) {
        try {
            VentaResponse response = ventaService.registrarVenta(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            logger.error("Error registrando venta", e);
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Error interno del servidor";
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", errorMessage, "statusCode", HttpStatus.BAD_REQUEST.value()));
        } catch (Exception e) {
            logger.error("Error inesperado registrando venta", e);
            String errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", errorMessage, "statusCode", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PostMapping("/credito-personal/simular")
    public ResponseEntity<?> simularCreditoPersonal(@RequestBody CreditoSimulacionRequest request) {
        try {
            CreditoSimulacionResponse response = ventaService.simularCreditoPersonal(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", e.getMessage(),
                    "statusCode", HttpStatus.BAD_REQUEST.value()));
        }
    }

    @PostMapping("/credito-personal")
    public ResponseEntity<?> registrarVentaCreditoPersonal(@RequestBody VentaCreditoPersonalRequest request) {
        try {
            VentaResponse response = ventaService.registrarVentaCreditoPersonal(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            logger.error("Error registrando venta con crédito personal", e);
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Error interno del servidor";
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", errorMessage, "statusCode", HttpStatus.BAD_REQUEST.value()));
        } catch (Exception e) {
            logger.error("Error inesperado registrando venta con crédito personal", e);
            String errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", errorMessage, "statusCode", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @GetMapping("/historial")
    public ResponseEntity<?> listarHistorial(
            @RequestParam(required = false) Long sucursalId,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        try {
            PageResponse<VentaHistorialItemResponse> response = ventaService.listarHistorial(
                    sucursalId, estado, desde, hasta, q, page, size);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage(), "statusCode", HttpStatus.BAD_REQUEST.value()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerVenta(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ventaService.obtenerVenta(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage(), "statusCode", HttpStatus.NOT_FOUND.value()));
        }
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<?> cancelarVenta(@PathVariable Long id, @RequestBody VentaCancelarRequest request) {
        try {
            VentaResponse response = ventaService.cancelarVenta(id, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Error cancelando venta {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage(), "statusCode", HttpStatus.BAD_REQUEST.value()));
        }
    }

    @PostMapping("/{id}/cambio-articulo")
    public ResponseEntity<?> cambiarArticulo(@PathVariable Long id, @RequestBody VentaCambioArticuloRequest request) {
        try {
            VentaResponse response = ventaService.cambiarArticulo(id, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Error en cambio de artículo venta {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage(), "statusCode", HttpStatus.BAD_REQUEST.value()));
        }
    }

    @GetMapping("/caja/cuentas")
    public ResponseEntity<List<CajaCuentaResponse>> listarCajas() {
        return ResponseEntity.ok(ventaService.listarCajas());
    }

    @GetMapping("/caja/movimientos")
    public ResponseEntity<List<CajaMovimientoResponse>> listarMovimientosCaja(
            @RequestParam(required = false) Long cuentaId) {
        return ResponseEntity.ok(ventaService.listarMovimientosCaja(cuentaId));
    }

    @GetMapping("/caja/sesiones/activa")
    public ResponseEntity<?> sesionActiva(@RequestParam Long cuentaId) {
        CajaSesionResponse sesion = cajaSesionService.obtenerSesionActiva(cuentaId);
        if (sesion == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(sesion);
    }

    @GetMapping("/caja/sesiones")
    public ResponseEntity<List<CajaSesionResponse>> listarSesionesCaja(@RequestParam Long cuentaId) {
        return ResponseEntity.ok(cajaSesionService.listarSesiones(cuentaId));
    }

    @PostMapping("/caja/sesiones/abrir")
    public ResponseEntity<?> abrirCaja(@RequestBody AbrirCajaRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(cajaSesionService.abrirCaja(request));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage(), "statusCode", HttpStatus.BAD_REQUEST.value()));
        }
    }

    @PostMapping("/caja/sesiones/{id}/cerrar")
    public ResponseEntity<?> cerrarCaja(@PathVariable Long id, @RequestBody CerrarCajaRequest request) {
        try {
            return ResponseEntity.ok(cajaSesionService.cerrarCaja(id, request));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage(), "statusCode", HttpStatus.BAD_REQUEST.value()));
        }
    }
}
