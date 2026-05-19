package com.vida.apirest.controller;

import com.vida.apirest.dto.ariticulo.ArticuloCompactResponse;
import com.vida.apirest.dto.ariticulo.ArticuloCreateRequest;
import com.vida.apirest.dto.usuario.LoginResponse;
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

    @PostMapping
    public ResponseEntity<Articulo> createArticulo(@RequestBody ArticuloCreateRequest request) {
        try {
            Articulo articulo = articuloService.createArticulo(request);
            return  ResponseEntity.status(HttpStatus.CREATED).body(articulo);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Articulo) Map.of(
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

    @GetMapping("/{id}")
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