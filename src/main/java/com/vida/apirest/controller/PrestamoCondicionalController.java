package com.vida.apirest.controller;

import com.vida.apirest.dto.prestamo.ConfirmarPrestamoCreditoRequest;
import com.vida.apirest.dto.prestamo.ConfirmarPrestamoRequest;
import com.vida.apirest.dto.prestamo.DevolverPrestamoDetallesRequest;
import com.vida.apirest.dto.prestamo.PrestamoCondicionalCreateRequest;
import com.vida.apirest.dto.prestamo.PrestamoCondicionalResponse;
import com.vida.apirest.security.Authz;
import com.vida.apirest.servicies.PrestamoCondicionalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prestamos-condicionales")
@RequiredArgsConstructor
@PreAuthorize(Authz.VER_O_GESTIONAR_VENTAS)
public class PrestamoCondicionalController {

    private final PrestamoCondicionalService prestamoCondicionalService;

    @PostMapping
    @PreAuthorize(Authz.GESTIONAR_VENTAS)
    public ResponseEntity<PrestamoCondicionalResponse> crear(@RequestBody PrestamoCondicionalCreateRequest request) {
        PrestamoCondicionalResponse response = prestamoCondicionalService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<PrestamoCondicionalResponse>> listar(
            @RequestParam(required = false) Long sucursalId,
            @RequestParam(required = false, defaultValue = "ACTIVOS") String estado) {
        return ResponseEntity.ok(prestamoCondicionalService.listar(sucursalId, estado));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PrestamoCondicionalResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(prestamoCondicionalService.obtener(id));
    }

    @PostMapping("/{id}/devolver")
    @PreAuthorize(Authz.GESTIONAR_VENTAS)
    public ResponseEntity<PrestamoCondicionalResponse> devolver(@PathVariable Long id) {
        return ResponseEntity.ok(prestamoCondicionalService.devolver(id));
    }

    @PostMapping("/{id}/devolver-detalles")
    @PreAuthorize(Authz.GESTIONAR_VENTAS)
    public ResponseEntity<PrestamoCondicionalResponse> devolverDetalles(
            @PathVariable Long id,
            @RequestBody DevolverPrestamoDetallesRequest request) {
        return ResponseEntity.ok(prestamoCondicionalService.devolverDetalles(id, request));
    }

    @PostMapping("/{id}/confirmar")
    @PreAuthorize(Authz.GESTIONAR_VENTAS)
    public ResponseEntity<PrestamoCondicionalResponse> confirmar(@PathVariable Long id, @RequestBody ConfirmarPrestamoRequest request) {
        return ResponseEntity.ok(prestamoCondicionalService.confirmarCompra(id, request));
    }

    @PostMapping("/{id}/confirmar-credito")
    @PreAuthorize(Authz.GESTIONAR_VENTAS)
    public ResponseEntity<PrestamoCondicionalResponse> confirmarCredito(
            @PathVariable Long id,
            @RequestBody ConfirmarPrestamoCreditoRequest request) {
        return ResponseEntity.ok(prestamoCondicionalService.confirmarCreditoPersonal(id, request));
    }
}
