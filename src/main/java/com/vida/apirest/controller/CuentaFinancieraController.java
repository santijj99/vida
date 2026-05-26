package com.vida.apirest.controller;

import com.vida.apirest.dto.finanzas.CreateCuentaFinancieraRequest;
import com.vida.apirest.dto.finanzas.CuentaFinancieraResponse;
import com.vida.apirest.servicies.CuentaFinancieraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cuenta-financiera")
public class CuentaFinancieraController {

    @Autowired
    private CuentaFinancieraService cuentaFinancieraService;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateCuentaFinancieraRequest request) {
        try {
            CuentaFinancieraResponse response = cuentaFinancieraService.createCuentaFinanciera(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Error interno del servidor";
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", errorMessage, "statusCode", HttpStatus.BAD_REQUEST.value()));
        }
    }

    @GetMapping
    public ResponseEntity<List<CuentaFinancieraResponse>> findAll() {
        return ResponseEntity.ok(cuentaFinancieraService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findById(@PathVariable Long id) {
        try {
            CuentaFinancieraResponse response = cuentaFinancieraService.findById(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Error interno del servidor";
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", errorMessage, "statusCode", HttpStatus.NOT_FOUND.value()));
        }
    }

    @GetMapping("/tipo/{tipo}")
    public ResponseEntity<List<CuentaFinancieraResponse>> findByTipo(@PathVariable String tipo) {
        return ResponseEntity.ok(cuentaFinancieraService.findByTipo(tipo));
    }
}