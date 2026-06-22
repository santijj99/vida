package com.vida.apirest.controller;

import com.vida.apirest.dto.config.ColumnasVistaRequest;
import com.vida.apirest.dto.config.ColumnasVistaResponse;
import com.vida.apirest.servicies.PreferenciaVistaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/configuracion-vista")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('VER_ARTICULOS')")
public class ConfiguracionVistaController {

    private final PreferenciaVistaService preferenciaVistaService;

    @GetMapping("/articulos/columnas")
    public ResponseEntity<ColumnasVistaResponse> obtenerColumnasArticulos() {
        return ResponseEntity.ok(preferenciaVistaService.obtenerColumnasArticulos());
    }

    @PutMapping("/articulos/columnas")
    public ResponseEntity<ColumnasVistaResponse> guardarColumnasArticulos(@RequestBody ColumnasVistaRequest request) {
        return ResponseEntity.ok(preferenciaVistaService.guardarColumnasArticulos(request.getColumnas()));
    }
}
