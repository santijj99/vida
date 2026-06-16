package com.vida.apirest.controller;

import com.vida.apirest.dto.almacen.DepositoCreateRequest;
import com.vida.apirest.dto.almacen.DepositoResponse;
import com.vida.apirest.model.almacen.Deposito;
import com.vida.apirest.model.almacen.Sucursal;
import com.vida.apirest.repositories.DepositoRepository;
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
@RequestMapping("/api/depositos")
@RequiredArgsConstructor
public class DepositoController {

    private final DepositoRepository depositoRepository;
    private final SucursalRepository sucursalRepository;

    @PostMapping
    public ResponseEntity<?> createDeposito(@RequestBody DepositoCreateRequest request) {
        try {
            Sucursal sucursal = sucursalRepository.findById(request.getSucursalId())
                    .orElseThrow(() -> new RuntimeException("Sucursal no encontrada con ID: " + request.getSucursalId()));

            Deposito deposito = new Deposito();
            deposito.setSucursal(sucursal);
            deposito.setNombre(request.getNombre());
            deposito.setCodigo(request.getCodigo());
            deposito.setUbicacion(request.getUbicacion());
            deposito.setDescripcion(request.getDescripcion());
            deposito.setTipo(Deposito.Tipo.valueOf(request.getTipo().toUpperCase()));

            return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(depositoRepository.save(deposito)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", "Tipo de depósito inválido. Valores: PRINCIPAL, SECUNDARIO, AUXILIAR, DISTRIBUCION",
                    "statusCode", HttpStatus.BAD_REQUEST.value()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", e.getMessage(),
                    "statusCode", HttpStatus.BAD_REQUEST.value()));
        }
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<DepositoResponse>> getAllDepositos(
            @RequestParam(required = false) Long sucursalId) {
        List<Deposito> depositos = sucursalId != null
                ? depositoRepository.findBySucursalIdOrderByNombreAsc(sucursalId)
                : depositoRepository.findAll();
        return ResponseEntity.ok(depositos.stream()
                .map(this::toResponse)
                .collect(Collectors.toList()));
    }

    private DepositoResponse toResponse(Deposito deposito) {
        DepositoResponse response = new DepositoResponse();
        response.setId(deposito.getId());
        if (deposito.getSucursal() != null) {
            response.setSucursalId(deposito.getSucursal().getId());
            response.setSucursalNombre(deposito.getSucursal().getNombre());
        }
        response.setNombre(deposito.getNombre());
        response.setCodigo(deposito.getCodigo());
        response.setUbicacion(deposito.getUbicacion());
        response.setDescripcion(deposito.getDescripcion());
        response.setTipo(deposito.getTipo() != null ? deposito.getTipo().name() : null);
        return response;
    }
}
