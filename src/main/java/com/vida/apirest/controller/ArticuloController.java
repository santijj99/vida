package com.vida.apirest.controller;

import com.vida.apirest.dto.ariticulo.ArticuloCompactResponse;
import com.vida.apirest.dto.ariticulo.ArticuloCreateRequest;
import com.vida.apirest.dto.ariticulo.ArticuloFiltrosResponse;
import com.vida.apirest.dto.ariticulo.ArticuloParaVentaResponse;
import com.vida.apirest.dto.ariticulo.ArticuloTablaRowResponse;
import com.vida.apirest.dto.ariticulo.VariantCreateRequest;
import com.vida.apirest.dto.ariticulo.VarianteCompactResponse;
import com.vida.apirest.model.almacen.Deposito;
import com.vida.apirest.model.almacen.Sucursal;
import com.vida.apirest.model.articulo.Articulo;
import com.vida.apirest.repositories.DepositoRepository;
import com.vida.apirest.repositories.SucursalRepository;
import com.vida.apirest.servicies.ArticuloService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/articulos")
@RequiredArgsConstructor
public class ArticuloController {

    private final ArticuloService articuloService;
    private final DepositoRepository depositoRepository;
    private final SucursalRepository sucursalRepository;

//    @PostMapping
//    public ResponseEntity<Articulo> createArticulo(@RequestBody ArticuloCreateRequest request) {
//        Articulo articulo = articuloService.createArticulo(request);
//        return ResponseEntity.ok(articulo);
//    }

    @GetMapping("/lista")
    public ResponseEntity<List<ArticuloCompactResponse>> listAll() {
        return ResponseEntity.ok(articuloService.findAllCompact());
    }

    @GetMapping("/tabla/filtros")
    public ResponseEntity<ArticuloFiltrosResponse> filtrosTabla() {
        return ResponseEntity.ok(articuloService.obtenerFiltrosTabla());
    }

    @GetMapping("/para-venta")
    public ResponseEntity<?> listParaVenta(@RequestParam Long sucursalId) {
        try {
            return ResponseEntity.ok(articuloService.findParaVenta(sucursalId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", e.getMessage(),
                    "statusCode", HttpStatus.BAD_REQUEST.value()
            ));
        }
    }

    @GetMapping("/tabla")
    public ResponseEntity<List<ArticuloTablaRowResponse>> listTabla(
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String subCategoria,
            @RequestParam(required = false) String genero,
            @RequestParam(required = false) String marca
    ) {
        return ResponseEntity.ok(articuloService.findAllTabla(categoria, subCategoria, genero, marca));
    }

    @PostMapping
    public ResponseEntity<?> createArticulo(@RequestBody ArticuloCreateRequest request) {
        try {
            Articulo articulo = articuloService.createArticulo(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    articuloService.getCompactById(articulo.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", e.getMessage(),
                    "statusCode", HttpStatus.BAD_REQUEST.value()
            ));
        }
    }

    @GetMapping("/depositos")
    public ResponseEntity<List<Deposito>> getDepositos() {
        List<Deposito> depositos = depositoRepository.findAll();
        return ResponseEntity.ok(depositos);
    }

    @GetMapping("/sucursales")
    public ResponseEntity<List<Sucursal>> getSucursales() {
        List<Sucursal> sucursales = sucursalRepository.findAll();
        return ResponseEntity.ok(sucursales);
    }

    @GetMapping
    public ResponseEntity<List<Articulo>> searchArticulos(
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) String marca,
            @RequestParam(required = false) String talle,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String modelo,
            @RequestParam(required = false) String genero
    ) {
        List<Articulo> results = articuloService.searchArticulos(codigo, marca, talle, color, categoria, modelo, genero);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id:[0-9]+}/compact")
    public ResponseEntity<?> getCompactById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(articuloService.getCompactById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "message", e.getMessage(),
                    "statusCode", HttpStatus.NOT_FOUND.value()
            ));
        }
    }

    @PostMapping("/{id:[0-9]+}/variantes")
    public ResponseEntity<?> agregarVariante(
            @PathVariable Long id,
            @RequestBody VariantCreateRequest request,
            @RequestParam(required = false) Long depositoId,
            @RequestParam(required = false) Long sucursalId
    ) {
        try {
            VarianteCompactResponse variante = articuloService.agregarVariante(
                    id, request, depositoId, sucursalId);
            return ResponseEntity.status(HttpStatus.CREATED).body(variante);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", e.getMessage(),
                    "statusCode", HttpStatus.BAD_REQUEST.value()
            ));
        }
    }

    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<Articulo> getById(@PathVariable Long id) {
        Articulo articulo = articuloService.getArticuloById(id);
        return ResponseEntity.ok(articulo);
    }

    @GetMapping("/by-codigo")
    public ResponseEntity<Articulo> getByCodigo(@RequestParam String codigo) {
        Articulo articulo = articuloService.getByCodigo(codigo);
        return ResponseEntity.ok(articulo);
    }

    @GetMapping("/by-codigo-compact")
    public ResponseEntity<ArticuloCompactResponse> getByCodigoCompact(@RequestParam String codigo) {
        ArticuloCompactResponse articulo = articuloService.getByCodigoCompact(codigo);
        return ResponseEntity.ok(articulo);
    }

    @GetMapping("/by-marca")
    public ResponseEntity<List<Articulo>> getByMarca(@RequestParam String marca) {
        return ResponseEntity.ok(articuloService.getByMarca(marca));
    }

    @GetMapping("/by-talle")
    public ResponseEntity<List<Articulo>> getByTalle(@RequestParam String talle) {
        return ResponseEntity.ok(articuloService.getByTalle(talle));
    }

    @GetMapping("/by-color")
    public ResponseEntity<List<Articulo>> getByColor(@RequestParam String color) {
        return ResponseEntity.ok(articuloService.getByColor(color));
    }
}