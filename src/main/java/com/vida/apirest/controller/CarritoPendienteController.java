package com.vida.apirest.controller;

import com.vida.apirest.dto.carrito.*;
import com.vida.apirest.security.Authz;
import com.vida.apirest.servicies.CarritoPendienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/carritos-pendientes")
@RequiredArgsConstructor
@PreAuthorize(Authz.VER_O_GESTIONAR_VENTAS)
public class CarritoPendienteController {

    private final CarritoPendienteService carritoPendienteService;

    @PostMapping
    @PreAuthorize(Authz.GESTIONAR_VENTAS)
    public ResponseEntity<CarritoPendienteResponse> crear(@RequestBody CarritoPendienteCreateRequest request) {
        CarritoPendienteResponse response = carritoPendienteService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<CarritoPendienteResponse>> listar(
            @RequestParam(required = false) Long sucursalId,
            @RequestParam(required = false, defaultValue = "PENDIENTES") String estado) {
        return ResponseEntity.ok(carritoPendienteService.listar(sucursalId, estado));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CarritoPendienteResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(carritoPendienteService.obtener(id));
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize(Authz.GESTIONAR_VENTAS)
    public ResponseEntity<CarritoPendienteResponse> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(carritoPendienteService.cancelar(id));
    }

    @PostMapping("/{id}/confirmar")
    @PreAuthorize(Authz.GESTIONAR_VENTAS)
    public ResponseEntity<CarritoPendienteResponse> confirmar(@PathVariable Long id, @RequestBody ConfirmarCarritoPendienteRequest request) {
        return ResponseEntity.ok(carritoPendienteService.confirmar(id, request));
    }

    @PostMapping("/{id}/confirmar-credito")
    @PreAuthorize(Authz.GESTIONAR_VENTAS)
    public ResponseEntity<CarritoPendienteResponse> confirmarCredito(
            @PathVariable Long id,
            @RequestBody ConfirmarCarritoPendienteCreditoRequest request) {
        return ResponseEntity.ok(carritoPendienteService.confirmarCredito(id, request));
    }
}
