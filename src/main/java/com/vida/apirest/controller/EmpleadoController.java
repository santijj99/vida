package com.vida.apirest.controller;

import com.vida.apirest.dto.empleado.CreateEmpleadoRequest;
import com.vida.apirest.dto.empleado.EmpleadoResponse;
import com.vida.apirest.security.Authz;
import com.vida.apirest.servicies.EmpleadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/empleado")
@RequiredArgsConstructor
@PreAuthorize(Authz.VER_O_GESTIONAR_EMPLEADOS)
public class EmpleadoController {

    private final EmpleadoService empleadoService;

    @GetMapping
    public ResponseEntity<List<EmpleadoResponse>> getAll() {
        return ResponseEntity.ok(empleadoService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmpleadoResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(empleadoService.findById(id));
    }

    @PostMapping
    @PreAuthorize(Authz.GESTIONAR_EMPLEADOS)
    public ResponseEntity<EmpleadoResponse> create(@RequestBody CreateEmpleadoRequest request) throws IOException {
        EmpleadoResponse response = empleadoService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping(value = "/{id}")
    @PreAuthorize(Authz.GESTIONAR_EMPLEADOS)
    public ResponseEntity<EmpleadoResponse> update(@PathVariable Long id, @RequestBody CreateEmpleadoRequest request) throws IOException {
        return ResponseEntity.ok(empleadoService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Authz.GESTIONAR_EMPLEADOS)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        empleadoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
