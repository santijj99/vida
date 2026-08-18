package com.vida.apirest.controller;

import com.vida.apirest.dto.empresa.EmpresaCreateRequest;
import com.vida.apirest.dto.empresa.EmpresaResponse;
import com.vida.apirest.dto.empresa.EmpresaUpdateRequest;
import com.vida.apirest.security.Authz;
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
@PreAuthorize(Authz.VER_O_GESTIONAR_ORGANIZACION)
public class EmpresaController {

    private final EmpresaService empresaService;

    @PostMapping
    @PreAuthorize(Authz.GESTIONAR_ORGANIZACION)
    public ResponseEntity<EmpresaResponse> createEmpresa(@RequestBody EmpresaCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(empresaService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmpresaResponse> getEmpresa(@PathVariable Long id) {
        return ResponseEntity.ok(empresaService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Authz.GESTIONAR_ORGANIZACION)
    public ResponseEntity<EmpresaResponse> updateEmpresa(
            @PathVariable Long id,
            @RequestBody EmpresaUpdateRequest request) {
        return ResponseEntity.ok(empresaService.update(id, request));
    }

    @GetMapping
    public ResponseEntity<List<EmpresaResponse>> getAllEmpresas() {
        return ResponseEntity.ok(empresaService.findAll());
    }
}
