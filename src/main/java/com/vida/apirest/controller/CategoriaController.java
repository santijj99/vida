package com.vida.apirest.controller;

import com.vida.apirest.dto.ariticulo.CategoriaResponse;
import com.vida.apirest.dto.ariticulo.CreateCategoriaRequest;
import com.vida.apirest.security.Authz;
import com.vida.apirest.servicies.CategoriaService;
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
@RequestMapping("/api/categorias")
@RequiredArgsConstructor
@PreAuthorize(Authz.VER_O_GESTIONAR_CATEGORIAS)
public class CategoriaController {

    private final CategoriaService categoriaService;

    @GetMapping
    public ResponseEntity<List<CategoriaResponse>> findAll() {
        return ResponseEntity.ok(categoriaService.findAll());
    }

    @PostMapping
    @PreAuthorize(Authz.GESTIONAR_CATEGORIAS)
    public ResponseEntity<CategoriaResponse> create(@RequestBody CreateCategoriaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoriaService.create(request));
    }
}
