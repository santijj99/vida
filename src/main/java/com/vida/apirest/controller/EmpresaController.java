package com.vida.apirest.controller;

import com.vida.apirest.dto.empresa.EmpresaCreateRequest;
import com.vida.apirest.model.empresa.Empresa;
import com.vida.apirest.repositories.EmpresaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/empresas")
@RequiredArgsConstructor
public class EmpresaController {

    private final EmpresaRepository empresaRepository;

    @PostMapping
    public ResponseEntity<Empresa> createEmpresa(@RequestBody EmpresaCreateRequest request) {
        Empresa empresa = new Empresa();
        empresa.setNombre(request.getNombre());
        empresa.setCodigo(request.getCodigo());
        empresa.setCuit(request.getCuit());
        empresa.setRazonSocial(request.getRazonSocial());
        empresa.setDomicilio(request.getDomicilio());
        empresa.setCiudad(request.getCiudad());
        empresa.setEstado(Empresa.EstadoEmpresa.ACTIVA);
        
        Empresa savedEmpresa = empresaRepository.save(empresa);
        return ResponseEntity.ok(savedEmpresa);
    }

    @GetMapping
    public ResponseEntity<List<Empresa>> getAllEmpresas() {
        List<Empresa> empresas = empresaRepository.findAll();
        return ResponseEntity.ok(empresas);
    }
}