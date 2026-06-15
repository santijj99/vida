package com.vida.apirest.controller;

import com.vida.apirest.dto.ariticulo.CreateTalleRequest;
import com.vida.apirest.dto.ariticulo.TalleResponse;
import com.vida.apirest.servicies.TalleService;
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
@RequestMapping("/api/talles")
@RequiredArgsConstructor
public class TalleController {

    private final TalleService talleService;

    @GetMapping
    public ResponseEntity<List<TalleResponse>> findAll() {
        return ResponseEntity.ok(talleService.findAll());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateTalleRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(talleService.create(request));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", e.getMessage(),
                    "statusCode", HttpStatus.BAD_REQUEST.value()
            ));
        }
    }
}
