package com.vida.apirest.controller;

import com.vida.apirest.dto.common.PageResponse;
import com.vida.apirest.dto.pedido.OrdenCompraRequest;
import com.vida.apirest.dto.pedido.OrdenCompraResponse;
import com.vida.apirest.dto.pedido.OrdenCompraVarianteLookupResponse;
import com.vida.apirest.dto.pedido.ResolverVariantesRequest;
import com.vida.apirest.security.Authz;
import com.vida.apirest.servicies.OrdenDeCompraService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ordenes-compra")
@RequiredArgsConstructor
@PreAuthorize(Authz.VER_O_GESTIONAR_PEDIDOS)
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
    public ResponseEntity<OrdenCompraResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ordenDeCompraService.findById(id));
    }

    @GetMapping("/variantes/codigo-barras/{codigo}")
    public ResponseEntity<OrdenCompraVarianteLookupResponse> buscarPorCodigoBarras(@PathVariable String codigo) {
        return ResponseEntity.ok(ordenDeCompraService.buscarPorCodigoBarras(codigo));
    }

    @PostMapping("/variantes/resolver")
    public ResponseEntity<List<OrdenCompraVarianteLookupResponse>> resolverCodigos(@RequestBody ResolverVariantesRequest request) {
        List<OrdenCompraVarianteLookupResponse> result = ordenDeCompraService.resolverCodigos(
                request != null ? request.getCodigosBarras() : null);
        return ResponseEntity.ok(result);
    }

    @PostMapping
    @PreAuthorize(Authz.GESTIONAR_PEDIDOS)
    public ResponseEntity<OrdenCompraResponse> crear(@RequestBody OrdenCompraRequest request) {
        OrdenCompraResponse response = ordenDeCompraService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id:[0-9]+}")
    @PreAuthorize(Authz.GESTIONAR_PEDIDOS)
    public ResponseEntity<OrdenCompraResponse> actualizar(@PathVariable Long id, @RequestBody OrdenCompraRequest request) {
        return ResponseEntity.ok(ordenDeCompraService.actualizar(id, request));
    }

    @PostMapping("/{id:[0-9]+}/confirmar")
    @PreAuthorize(Authz.GESTIONAR_PEDIDOS)
    public ResponseEntity<OrdenCompraResponse> confirmar(@PathVariable Long id) {
        return ResponseEntity.ok(ordenDeCompraService.confirmar(id));
    }

    @PostMapping("/{id:[0-9]+}/cancelar")
    @PreAuthorize(Authz.GESTIONAR_PEDIDOS)
    public ResponseEntity<OrdenCompraResponse> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(ordenDeCompraService.cancelar(id));
    }
}
