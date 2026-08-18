package com.vida.apirest.controller;

import com.vida.apirest.dto.cliente.ClienteResponse;
import com.vida.apirest.dto.cliente.CreateClienteRequest;
import com.vida.apirest.dto.cliente.CreateClienteSimpleRequest;
import com.vida.apirest.dto.cliente.CreateClienteWithGaranteAndContactoRequest;
import com.vida.apirest.dto.common.PageResponse;
import com.vida.apirest.security.Authz;
import com.vida.apirest.servicies.ClienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cliente")
@RequiredArgsConstructor
@PreAuthorize(Authz.VER_O_GESTIONAR_CLIENTES)
public class ClienteController {

    private final ClienteService clienteService;

    @GetMapping
    public ResponseEntity<List<ClienteResponse>> getAll() {
        return ResponseEntity.ok(clienteService.findAll());
    }

    @GetMapping("/pagina")
    public ResponseEntity<PageResponse<ClienteResponse>> getPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(clienteService.findPage(q, page, size));
    }

    @GetMapping("/dni/{dni}")
    public ResponseEntity<ClienteResponse> getByDni(@PathVariable String dni) {
        return ResponseEntity.ok(clienteService.findByDni(dni));
    }

    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<ClienteResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.findById(id));
    }

    @PostMapping
    @PreAuthorize(Authz.GESTIONAR_CLIENTES)
    public ResponseEntity<ClienteResponse> create(@RequestBody CreateClienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.create(request));
    }

    @PostMapping("/solo")
    @PreAuthorize(Authz.GESTIONAR_CLIENTES)
    public ResponseEntity<ClienteResponse> createClienteOnly(@RequestBody CreateClienteSimpleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.createClienteOnly(request));
    }

    @PostMapping("/con-garante-contacto")
    @PreAuthorize(Authz.GESTIONAR_CLIENTES)
    public ResponseEntity<ClienteResponse> createClienteWithGaranteAndContacto(
            @RequestBody CreateClienteWithGaranteAndContactoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.createClienteWithGaranteAndContacto(request));
    }

    @PutMapping("/{id:[0-9]+}")
    @PreAuthorize(Authz.GESTIONAR_CLIENTES)
    public ResponseEntity<ClienteResponse> update(@PathVariable Long id, @RequestBody CreateClienteRequest request) {
        return ResponseEntity.ok(clienteService.update(id, request));
    }

    @DeleteMapping("/{id:[0-9]+}")
    @PreAuthorize(Authz.GESTIONAR_CLIENTES)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        clienteService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
