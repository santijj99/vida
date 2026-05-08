package com.vida.apirest.controller;

import com.vida.apirest.dto.almacen.SucursalCreateRequest;
import com.vida.apirest.model.almacen.Sucursal;
import com.vida.apirest.model.empresa.Empresa;
import com.vida.apirest.repositories.EmpresaRepository;
import com.vida.apirest.repositories.SucursalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sucursales")
@RequiredArgsConstructor
public class SucursalController {

    private final SucursalRepository sucursalRepository;
    private final EmpresaRepository empresaRepository;

    @PostMapping
    public ResponseEntity<Sucursal> createSucursal(@RequestBody SucursalCreateRequest request) {
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
        
        Sucursal savedSucursal = sucursalRepository.save(sucursal);
        return ResponseEntity.ok(savedSucursal);
    }

    @GetMapping
    public ResponseEntity<List<Sucursal>> getAllSucursales() {
        List<Sucursal> sucursales = sucursalRepository.findAll();
        return ResponseEntity.ok(sucursales);
    }
}