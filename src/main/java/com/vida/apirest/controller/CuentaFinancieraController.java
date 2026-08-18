package com.vida.apirest.controller;

import com.vida.apirest.dto.finanzas.CreateCuentaFinancieraRequest;
import com.vida.apirest.dto.finanzas.CuentaFinancieraResponse;
import com.vida.apirest.dto.finanzas.TransferenciaCuentaRequest;
import com.vida.apirest.dto.finanzas.TransferenciaCuentaResponse;
import com.vida.apirest.security.Authz;
import com.vida.apirest.servicies.CuentaFinancieraService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cuenta-financiera")
@RequiredArgsConstructor
@PreAuthorize(Authz.VER_O_GESTIONAR_ORG_O_CAJA)
public class CuentaFinancieraController {

    private final CuentaFinancieraService cuentaFinancieraService;

    @PostMapping
    @PreAuthorize(Authz.GESTIONAR_ORGANIZACION)
    public ResponseEntity<CuentaFinancieraResponse> create(@RequestBody CreateCuentaFinancieraRequest request) {
        CuentaFinancieraResponse response = cuentaFinancieraService.createCuentaFinanciera(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/transferir")
    @PreAuthorize(Authz.GESTIONAR_CAJA_O_ORGANIZACION)
    public ResponseEntity<TransferenciaCuentaResponse> transferir(@RequestBody TransferenciaCuentaRequest request) {
        return ResponseEntity.ok(cuentaFinancieraService.transferir(request));
    }

    @GetMapping
    public ResponseEntity<List<CuentaFinancieraResponse>> findAll() {
        return ResponseEntity.ok(cuentaFinancieraService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CuentaFinancieraResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(cuentaFinancieraService.findById(id));
    }

    @GetMapping("/tipo/{tipo}")
    public ResponseEntity<List<CuentaFinancieraResponse>> findByTipo(@PathVariable String tipo) {
        return ResponseEntity.ok(cuentaFinancieraService.findByTipo(tipo));
    }
}
