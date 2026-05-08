package com.vida.apirest.controller;

import com.vida.apirest.dto.almacen.DepositoCreateRequest;
import com.vida.apirest.model.almacen.Deposito;
import com.vida.apirest.model.almacen.Sucursal;
import com.vida.apirest.repositories.DepositoRepository;
import com.vida.apirest.repositories.SucursalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/depositos")
@RequiredArgsConstructor
public class DepositoController {

    private final DepositoRepository depositoRepository;
    private final SucursalRepository sucursalRepository;

    @PostMapping
    public ResponseEntity<Deposito> createDeposito(@RequestBody DepositoCreateRequest request) {
        Sucursal sucursal = sucursalRepository.findById(request.getSucursalId())
                .orElseThrow(() -> new RuntimeException("Sucursal no encontrada con ID: " + request.getSucursalId()));
        
        Deposito deposito = new Deposito();
        deposito.setSucursal(sucursal);
        deposito.setNombre(request.getNombre());
        deposito.setCodigo(request.getCodigo());
        deposito.setUbicacion(request.getUbicacion());
        deposito.setDescripcion(request.getDescripcion());
        
        // Convertir string a enum, con validación
        try {
            Deposito.Tipo tipo = Deposito.Tipo.valueOf(request.getTipo().toUpperCase());
            deposito.setTipo(tipo);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Tipo de depósito inválido. Valores válidos: PRINCIPAL, SECUNDARIO, AUXILIAR, DISTRIBUCION");
        }
        
        Deposito savedDeposito = depositoRepository.save(deposito);
        return ResponseEntity.ok(savedDeposito);
    }

    @GetMapping
    public ResponseEntity<List<Deposito>> getAllDepositos() {
        List<Deposito> depositos = depositoRepository.findAll();
        return ResponseEntity.ok(depositos);
    }
}