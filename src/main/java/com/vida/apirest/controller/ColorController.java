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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/colores")
@RequiredArgsConstructor
public class ColorController {

    private final ColorService colorService;

    @GetMapping
    public ResponseEntity<List<ColorResponse>> findAll() {
        return ResponseEntity.ok(colorService.findAll());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateColorRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(colorService.create(request));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", e.getMessage(),
                    "statusCode", HttpStatus.BAD_REQUEST.value()
            ));
        }
    }
}
