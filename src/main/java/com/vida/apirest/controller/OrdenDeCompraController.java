package com.vida.apirest.controller;

import com.vida.apirest.dto.common.PageResponse;
import com.vida.apirest.dto.pedido.OrdenCompraRequest;
import com.vida.apirest.dto.pedido.OrdenCompraResponse;
import com.vida.apirest.dto.pedido.OrdenCompraVarianteLookupResponse;
import com.vida.apirest.dto.pedido.ResolverVariantesRequest;
import com.vida.apirest.servicies.OrdenDeCompraService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ordenes-compra")
@RequiredArgsConstructor
public class OrdenDeCompraController {

    private final OrdenDeCompraService ordenDeCompraService;

    @GetMapping("/pagina")
    public ResponseEntity<PageResponse<OrdenCompraResponse>> pagina(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String estado) {
        return ResponseEntity.ok(ordenDeCompraService.findPage(q, estado, page, size));
    }

    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<?> obtener(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ordenDeCompraService.findById(id));
        } catch (RuntimeException e) {
            return error(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @GetMapping("/variantes/codigo-barras/{codigo}")
    public ResponseEntity<?> buscarPorCodigoBarras(@PathVariable String codigo) {
        try {
            return ResponseEntity.ok(ordenDeCompraService.buscarPorCodigoBarras(codigo));
        } catch (RuntimeException e) {
            return error(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @PostMapping("/variantes/resolver")
    public ResponseEntity<?> resolverCodigos(@RequestBody ResolverVariantesRequest request) {
        try {
            List<OrdenCompraVarianteLookupResponse> result = ordenDeCompraService.resolverCodigos(
                    request != null ? request.getCodigosBarras() : null);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody OrdenCompraRequest request) {
        try {
            OrdenCompraResponse response = ordenDeCompraService.crear(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PutMapping("/{id:[0-9]+}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody OrdenCompraRequest request) {
        try {
            return ResponseEntity.ok(ordenDeCompraService.actualizar(id, request));
        } catch (RuntimeException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/{id:[0-9]+}/confirmar")
    public ResponseEntity<?> confirmar(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ordenDeCompraService.confirmar(id));
        } catch (RuntimeException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/{id:[0-9]+}/cancelar")
    public ResponseEntity<?> cancelar(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ordenDeCompraService.cancelar(id));
        } catch (RuntimeException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "message", message != null ? message : "Error",
                "statusCode", status.value()));
    }
}
