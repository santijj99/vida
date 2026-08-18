package com.vida.apirest.controller;

import com.vida.apirest.dto.config.ColumnasVistaRequest;
import com.vida.apirest.dto.config.ColumnasVistaResponse;
import com.vida.apirest.security.Authz;
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
public class ConfiguracionVistaController {

    private final PreferenciaVistaService preferenciaVistaService;

    @GetMapping("/articulos/columnas")
    @PreAuthorize(Authz.VER_O_GESTIONAR_ARTICULOS)
    public ResponseEntity<ColumnasVistaResponse> obtenerColumnasArticulos() {
        return ResponseEntity.ok(preferenciaVistaService.obtenerColumnasArticulos());
    }

    @PutMapping("/articulos/columnas")
    @PreAuthorize(Authz.VER_O_GESTIONAR_ARTICULOS)
    public ResponseEntity<ColumnasVistaResponse> guardarColumnasArticulos(@RequestBody ColumnasVistaRequest request) {
        return ResponseEntity.ok(preferenciaVistaService.guardarColumnasArticulos(request.getColumnas()));
    }

    @GetMapping("/pedidos/columnas")
    @PreAuthorize(Authz.VER_O_GESTIONAR_PEDIDOS)
    public ResponseEntity<ColumnasVistaResponse> obtenerColumnasPedidos() {
        return ResponseEntity.ok(preferenciaVistaService.obtenerColumnasPedidos());
    }

    @PutMapping("/pedidos/columnas")
    @PreAuthorize(Authz.VER_O_GESTIONAR_PEDIDOS)
    public ResponseEntity<ColumnasVistaResponse> guardarColumnasPedidos(@RequestBody ColumnasVistaRequest request) {
        return ResponseEntity.ok(preferenciaVistaService.guardarColumnasPedidos(request.getColumnas()));
    }
}
