package com.vida.apirest.controller;

import com.vida.apirest.dto.almacen.SucursalCreateRequest;
import com.vida.apirest.dto.almacen.SucursalResponse;
import com.vida.apirest.model.almacen.Sucursal;
import com.vida.apirest.model.empresa.Empresa;
import com.vida.apirest.repositories.EmpresaRepository;
import com.vida.apirest.repositories.SucursalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sucursales")
@RequiredArgsConstructor
public class SucursalController {

    private final SucursalRepository sucursalRepository;
    private final EmpresaRepository empresaRepository;

    @PostMapping
    public ResponseEntity<?> createSucursal(@RequestBody SucursalCreateRequest request) {
        try {
            Empresa empresa = empresaRepository.findById(request.getEmpresaId())
                    .orElseThrow(() -> new RuntimeException("Empresa no encontrada con ID: " + request.getEmpresaId()));

            Sucursal sucursal = new Sucursal();
            sucursal.setEmpresa(empresa);
            sucursal.setNombre(request.getNombre());
            sucursal.setCodigo(request.getCodigo());
            sucursal.setDomicilio(request.getDomicilio());
            sucursal.setCiudad(request.getCiudad());
            sucursal.setProvincia(request.getProvincia());
            sucursal.setEstado(Sucursal.EstadoSucursal.ACTIVA);

            return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(sucursalRepository.save(sucursal)));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", e.getMessage(),
                    "statusCode", HttpStatus.BAD_REQUEST.value()));
        }
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<SucursalResponse>> getAllSucursales() {
        return ResponseEntity.ok(sucursalRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList()));
    }

    private SucursalResponse toResponse(Sucursal sucursal) {
        SucursalResponse response = new SucursalResponse();
        response.setId(sucursal.getId());
        if (sucursal.getEmpresa() != null) {
            response.setEmpresaId(sucursal.getEmpresa().getId());
            response.setEmpresaNombre(sucursal.getEmpresa().getNombre());
        }
        response.setNombre(sucursal.getNombre());
        response.setCodigo(sucursal.getCodigo());
        response.setDomicilio(sucursal.getDomicilio());
        response.setCiudad(sucursal.getCiudad());
        response.setProvincia(sucursal.getProvincia());
        response.setEstado(sucursal.getEstado() != null ? sucursal.getEstado().name() : null);
        return response;
    }
}
