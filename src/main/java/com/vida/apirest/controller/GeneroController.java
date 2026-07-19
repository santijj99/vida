package com.vida.apirest.controller;

import com.vida.apirest.dto.ariticulo.CreateGeneroRequest;
import com.vida.apirest.dto.ariticulo.GeneroResponse;
import com.vida.apirest.servicies.GeneroService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/generos")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('VER_GENEROS')")
public class GeneroController {

    private final GeneroService generoService;

    @GetMapping
    public ResponseEntity<List<GeneroResponse>> findAll() {
        return ResponseEntity.ok(generoService.findAll());
    }

    @PostMapping
    public ResponseEntity<GeneroResponse> create(@RequestBody CreateGeneroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(generoService.create(request));
    }
}
