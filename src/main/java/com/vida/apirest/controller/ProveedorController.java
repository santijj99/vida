package com.vida.apirest.controller;

import com.vida.apirest.dto.common.PageResponse;
import com.vida.apirest.dto.proveedor.ProveedorRequest;
import com.vida.apirest.dto.proveedor.ProveedorResponse;
import com.vida.apirest.security.Authz;
import com.vida.apirest.servicies.ProveedorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/proveedores")
@RequiredArgsConstructor
@PreAuthorize(Authz.VER_O_GESTIONAR_PROVEEDORES)
public class ProveedorController {

    private final ProveedorService proveedorService;

    @GetMapping
    public ResponseEntity<List<ProveedorResponse>> listarActivos() {
        return ResponseEntity.ok(proveedorService.findAllActivos());
    }

    @GetMapping("/pagina")
    public ResponseEntity<PageResponse<ProveedorResponse>> pagina(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "true") boolean soloActivos) {
        return ResponseEntity.ok(proveedorService.findPage(q, page, size, soloActivos));
    }

    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<ProveedorResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(proveedorService.findById(id));
    }

    @PostMapping
    @PreAuthorize(Authz.GESTIONAR_PROVEEDORES)
    public ResponseEntity<ProveedorResponse> crear(@RequestBody ProveedorRequest request) {
        ProveedorResponse response = proveedorService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id:[0-9]+}")
    @PreAuthorize(Authz.GESTIONAR_PROVEEDORES)
    public ResponseEntity<ProveedorResponse> actualizar(@PathVariable Long id, @RequestBody ProveedorRequest request) {
        return ResponseEntity.ok(proveedorService.update(id, request));
    }

    @DeleteMapping("/{id:[0-9]+}")
    @PreAuthorize(Authz.GESTIONAR_PROVEEDORES)
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        proveedorService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
