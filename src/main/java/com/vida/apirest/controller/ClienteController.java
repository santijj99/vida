package com.vida.apirest.controller;

import com.vida.apirest.dto.cliente.ClienteResponse;
import com.vida.apirest.dto.cliente.CreateClienteRequest;
import com.vida.apirest.servicies.ClienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cliente")
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    @GetMapping
    public ResponseEntity<List<ClienteResponse>> getAll() {
        List<ClienteResponse> clientes = clienteService.findAll();
        return ResponseEntity.ok(clientes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            ClienteResponse response = clienteService.findById(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage(), "statusCode", HttpStatus.NOT_FOUND.value()));
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateClienteRequest request) {
        try {
            ClienteResponse response = clienteService.create(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage(), "statusCode", HttpStatus.BAD_REQUEST.value()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody CreateClienteRequest request) {
        try {
            ClienteResponse response = clienteService.update(id, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage(), "statusCode", HttpStatus.NOT_FOUND.value()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            clienteService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage(), "statusCode", HttpStatus.NOT_FOUND.value()));
        }
    }
}
