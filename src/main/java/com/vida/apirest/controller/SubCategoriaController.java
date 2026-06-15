package com.vida.apirest.controller;

import com.vida.apirest.dto.ariticulo.CreateSubCategoriaRequest;
import com.vida.apirest.dto.ariticulo.SubCategoriaResponse;
import com.vida.apirest.servicies.SubCategoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sub-categorias")
@RequiredArgsConstructor
public class SubCategoriaController {

    private final SubCategoriaService subCategoriaService;

    @GetMapping
    public ResponseEntity<List<SubCategoriaResponse>> findAll() {
        return ResponseEntity.ok(subCategoriaService.findAll());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateSubCategoriaRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(subCategoriaService.create(request));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", e.getMessage(),
                    "statusCode", HttpStatus.BAD_REQUEST.value()
            ));
        }
    }
}
