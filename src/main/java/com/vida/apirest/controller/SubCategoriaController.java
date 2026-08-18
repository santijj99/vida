package com.vida.apirest.controller;

import com.vida.apirest.dto.ariticulo.CreateSubCategoriaRequest;
import com.vida.apirest.dto.ariticulo.SubCategoriaResponse;
import com.vida.apirest.security.Authz;
import com.vida.apirest.servicies.SubCategoriaService;
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
@RequestMapping("/api/sub-categorias")
@RequiredArgsConstructor
@PreAuthorize(Authz.VER_O_GESTIONAR_SUBCATEGORIAS)
public class SubCategoriaController {

    private final SubCategoriaService subCategoriaService;

    @GetMapping
    public ResponseEntity<List<SubCategoriaResponse>> findAll() {
        return ResponseEntity.ok(subCategoriaService.findAll());
    }

    @PostMapping
    @PreAuthorize(Authz.GESTIONAR_SUBCATEGORIAS)
    public ResponseEntity<SubCategoriaResponse> create(@RequestBody CreateSubCategoriaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(subCategoriaService.create(request));
    }
}
