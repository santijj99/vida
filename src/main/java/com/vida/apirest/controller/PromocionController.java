package com.vida.apirest.controller;

import com.vida.apirest.dto.ariticulo.CreatePromocionRequest;
import com.vida.apirest.dto.ariticulo.PromocionResponse;
import com.vida.apirest.servicies.PromocionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/promociones")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('VER_PROMOCIONES')")
public class PromocionController {

    private final PromocionService promocionService;

    @GetMapping
    public ResponseEntity<List<PromocionResponse>> findAll() {
        return ResponseEntity.ok(promocionService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PromocionResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(promocionService.findById(id));
    }

    @PostMapping
    public ResponseEntity<PromocionResponse> create(@RequestBody CreatePromocionRequest request) {
        PromocionResponse response = promocionService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PromocionResponse> update(@PathVariable Long id, @RequestBody CreatePromocionRequest request) {
        return ResponseEntity.ok(promocionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        promocionService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Promoción eliminada"));
    }
}
