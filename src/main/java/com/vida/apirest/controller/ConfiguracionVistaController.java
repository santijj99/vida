package com.vida.apirest.controller;

import com.vida.apirest.dto.config.ColumnasVistaRequest;
import com.vida.apirest.dto.config.ColumnasVistaResponse;
import com.vida.apirest.servicies.PreferenciaVistaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/configuracion-vista")
@RequiredArgsConstructor
public class ConfiguracionVistaController {

    private final PreferenciaVistaService preferenciaVistaService;

    @GetMapping("/articulos/columnas")
    public ResponseEntity<ColumnasVistaResponse> obtenerColumnasArticulos() {
        return ResponseEntity.ok(preferenciaVistaService.obtenerColumnasArticulos());
    }

    @PutMapping("/articulos/columnas")
    public ResponseEntity<?> guardarColumnasArticulos(@RequestBody ColumnasVistaRequest request) {
        try {
            ColumnasVistaResponse response = preferenciaVistaService.guardarColumnasArticulos(request.getColumnas());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", e.getMessage(),
                    "statusCode", HttpStatus.BAD_REQUEST.value()
            ));
        }
    }
}
