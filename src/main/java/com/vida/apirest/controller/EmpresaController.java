package com.vida.apirest.controller;

import com.vida.apirest.dto.empresa.EmpresaCreateRequest;
import com.vida.apirest.dto.empresa.EmpresaResponse;
import com.vida.apirest.model.empresa.Empresa;
import com.vida.apirest.repositories.EmpresaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/empresas")
@RequiredArgsConstructor
public class EmpresaController {

    private final EmpresaRepository empresaRepository;

    @PostMapping
    public ResponseEntity<?> createEmpresa(@RequestBody EmpresaCreateRequest request) {
        try {
            Empresa empresa = new Empresa();
            empresa.setNombre(request.getNombre());
            empresa.setCodigo(request.getCodigo());
            empresa.setCuit(request.getCuit());
            empresa.setRazonSocial(request.getRazonSocial());
            empresa.setDomicilio(request.getDomicilio());
            empresa.setCiudad(request.getCiudad());
            empresa.setEstado(Empresa.EstadoEmpresa.ACTIVA);
            return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(empresaRepository.save(empresa)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", e.getMessage() != null ? e.getMessage() : "Error al crear empresa",
                    "statusCode", HttpStatus.BAD_REQUEST.value()));
        }
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<EmpresaResponse>> getAllEmpresas() {
        return ResponseEntity.ok(empresaRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList()));
    }

    private EmpresaResponse toResponse(Empresa empresa) {
        EmpresaResponse response = new EmpresaResponse();
        response.setId(empresa.getId());
        response.setNombre(empresa.getNombre());
        response.setCodigo(empresa.getCodigo());
        response.setCuit(empresa.getCuit());
        response.setRazonSocial(empresa.getRazonSocial());
        response.setDomicilio(empresa.getDomicilio());
        response.setCiudad(empresa.getCiudad());
        response.setEstado(empresa.getEstado() != null ? empresa.getEstado().name() : null);
        return response;
    }
}
