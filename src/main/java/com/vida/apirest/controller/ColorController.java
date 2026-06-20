package com.vida.apirest.controller;

import com.vida.apirest.dto.ariticulo.ColorResponse;
import com.vida.apirest.dto.ariticulo.CreateColorRequest;
import com.vida.apirest.servicies.ColorService;
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
@RequestMapping("/api/colores")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('VER_COLORES')")
public class ColorController {

    private final ColorService colorService;

    @GetMapping
    public ResponseEntity<List<ColorResponse>> findAll() {
        return ResponseEntity.ok(colorService.findAll());
    }

    @PostMapping
    public ResponseEntity<ColorResponse> create(@RequestBody CreateColorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(colorService.create(request));
    }
}
