package com.vida.apirest.controller;

import com.vida.apirest.dto.empresa.EmpresaCreateRequest;
import com.vida.apirest.dto.empresa.EmpresaResponse;
import com.vida.apirest.servicies.EmpresaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/empresas")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('VER_ORGANIZACION')")
public class EmpresaController {

    private final EmpresaService empresaService;

    @PostMapping
    public ResponseEntity<EmpresaResponse> createEmpresa(@RequestBody EmpresaCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(empresaService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<EmpresaResponse>> getAllEmpresas() {
        return ResponseEntity.ok(empresaService.findAll());
    }
}
