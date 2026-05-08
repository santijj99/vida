package com.vida.apirest.controller;

import com.vida.apirest.dto.venta.CajaCuentaResponse;
import com.vida.apirest.dto.venta.CajaMovimientoResponse;
import com.vida.apirest.dto.venta.VentaCreateRequest;
import com.vida.apirest.dto.venta.VentaResponse;
import com.vida.apirest.servicies.VentaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/venta")
public class VentaController {

    private static final Logger logger = LoggerFactory.getLogger(VentaController.class);

    @Autowired
    private VentaService ventaService;

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

    @GetMapping("/caja/cuentas")
    public ResponseEntity<List<CajaCuentaResponse>> listarCajas() {
        return ResponseEntity.ok(ventaService.listarCajas());
    }

    @GetMapping("/caja/movimientos")
    public ResponseEntity<List<CajaMovimientoResponse>> listarMovimientosCaja() {
        return ResponseEntity.ok(ventaService.listarMovimientosCaja());
    }
}
