package com.vida.apirest.controller;

import com.vida.apirest.dto.ariticulo.ClasificacionResponse;
import com.vida.apirest.dto.ariticulo.CreateClasificacionRequest;
import com.vida.apirest.servicies.ClasificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/clasificaciones")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('VER_SUBCATEGORIAS')")
public class ClasificacionController {

    private final ClasificacionService clasificacionService;

    @GetMapping
    public ResponseEntity<List<ClasificacionResponse>> findAll() {
        return ResponseEntity.ok(clasificacionService.findAll());
    }

    @PostMapping
    public ResponseEntity<ClasificacionResponse> create(@RequestBody CreateClasificacionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clasificacionService.create(request));
    }
}
