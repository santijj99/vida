package com.vida.apirest.controller;

import com.vida.apirest.dto.almacen.DepositoCreateRequest;
import com.vida.apirest.dto.almacen.DepositoResponse;
import com.vida.apirest.servicies.DepositoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/depositos")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('VER_ORGANIZACION')")
public class DepositoController {

    private final DepositoService depositoService;

    @PostMapping
    public ResponseEntity<DepositoResponse> createDeposito(@RequestBody DepositoCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(depositoService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DepositoResponse> updateDeposito(
            @PathVariable Long id,
            @RequestBody DepositoCreateRequest request) {
        return ResponseEntity.ok(depositoService.update(id, request));
    }

    @GetMapping
    public ResponseEntity<List<DepositoResponse>> getAllDepositos(
            @RequestParam(required = false) Long sucursalId) {
        return ResponseEntity.ok(depositoService.findAll(sucursalId));
    }
}
