package com.vida.apirest.controller;

import com.vida.apirest.dto.ariticulo.ArticuloCreateRequest;
import com.vida.apirest.model.almacen.Deposito;
import com.vida.apirest.model.almacen.Sucursal;
import com.vida.apirest.model.articulo.Articulo;
import com.vida.apirest.repositories.DepositoRepository;
import com.vida.apirest.repositories.SucursalRepository;
import com.vida.apirest.servicies.ArticuloService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/articulos")
@RequiredArgsConstructor
public class ArticuloController {

    private final ArticuloService articuloService;
    private final DepositoRepository depositoRepository;
    private final SucursalRepository sucursalRepository;

    @PostMapping
    public ResponseEntity<Articulo> createArticulo(@RequestBody ArticuloCreateRequest request) {
        Articulo articulo = articuloService.createArticulo(request);
        return ResponseEntity.ok(articulo);
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
}