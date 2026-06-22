package com.vida.apirest.controller;

import com.vida.apirest.dto.almacen.SucursalCreateRequest;
import com.vida.apirest.dto.almacen.SucursalResponse;
import com.vida.apirest.servicies.SucursalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sucursales")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('VER_ORGANIZACION')")
public class SucursalController {

    private final SucursalService sucursalService;

    @PostMapping
    public ResponseEntity<SucursalResponse> createSucursal(@RequestBody SucursalCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sucursalService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<SucursalResponse>> getAllSucursales() {
        return ResponseEntity.ok(sucursalService.findAll());
    }
}
