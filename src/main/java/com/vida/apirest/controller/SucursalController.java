package com.vida.apirest.controller;

import com.vida.apirest.dto.almacen.SucursalCreateRequest;
import com.vida.apirest.dto.almacen.SucursalResponse;
import com.vida.apirest.dto.empleado.EmpleadoResponse;
import com.vida.apirest.servicies.SucursalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sucursales")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('VER_ORGANIZACION')")
public class SucursalController {

    private final SucursalService sucursalService;

    @PostMapping
    public ResponseEntity<SucursalResponse> createSucursal(@RequestBody SucursalCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sucursalService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<SucursalResponse>> getAllSucursales() {
        return ResponseEntity.ok(sucursalService.findAll());
    }

    @GetMapping("/{id}/empleados")
    public ResponseEntity<List<EmpleadoResponse>> listarEmpleados(@PathVariable Long id) {
        return ResponseEntity.ok(sucursalService.listarEmpleados(id));
    }

    @GetMapping("/{id}/empleados/disponibles")
    public ResponseEntity<List<EmpleadoResponse>> listarEmpleadosDisponibles(@PathVariable Long id) {
        return ResponseEntity.ok(sucursalService.listarEmpleadosDisponibles(id));
    }

    @PostMapping("/{id}/empleados")
    public ResponseEntity<EmpleadoResponse> asignarEmpleado(
            @PathVariable Long id,
            @RequestBody Map<String, Long> body) {
        Long empleadoId = body.get("empleadoId");
        if (empleadoId == null) {
            throw new IllegalArgumentException("empleadoId es requerido");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(sucursalService.asignarEmpleado(id, empleadoId));
    }

    @DeleteMapping("/{id}/empleados/{empleadoId}")
    public ResponseEntity<Void> quitarEmpleado(@PathVariable Long id, @PathVariable Long empleadoId) {
        sucursalService.quitarEmpleado(id, empleadoId);
        return ResponseEntity.noContent().build();
    }
}
