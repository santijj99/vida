package com.vida.apirest.controller;

import com.vida.apirest.dto.finanzas.GastoCategoriaRequest;
import com.vida.apirest.dto.finanzas.GastoCategoriaResponse;
import com.vida.apirest.servicies.GastoCategoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gastos-categorias")
@RequiredArgsConstructor
public class GastoCategoriaController {

    private final GastoCategoriaService gastoCategoriaService;

    @GetMapping
    @PreAuthorize("hasAuthority('VER_GASTOS')")
    public ResponseEntity<List<GastoCategoriaResponse>> listar(
            @RequestParam(required = false, defaultValue = "true") Boolean soloActivas) {
        return ResponseEntity.ok(gastoCategoriaService.listar(soloActivas));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VER_GASTOS')")
    public ResponseEntity<GastoCategoriaResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(gastoCategoriaService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('GESTIONAR_GASTOS')")
    public ResponseEntity<GastoCategoriaResponse> crear(@RequestBody GastoCategoriaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gastoCategoriaService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('GESTIONAR_GASTOS')")
    public ResponseEntity<GastoCategoriaResponse> actualizar(@PathVariable Long id, @RequestBody GastoCategoriaRequest request) {
        return ResponseEntity.ok(gastoCategoriaService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('GESTIONAR_GASTOS')")
    public ResponseEntity<Map<String, String>> desactivar(@PathVariable Long id) {
        gastoCategoriaService.desactivar(id);
        return ResponseEntity.ok(Map.of("message", "Categoría desactivada"));
    }
}
