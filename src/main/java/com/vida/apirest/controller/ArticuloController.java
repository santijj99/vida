package com.vida.apirest.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vida.apirest.dto.ariticulo.ArticuloCompactResponse;
import com.vida.apirest.dto.ariticulo.ArticuloCreateRequest;
import com.vida.apirest.dto.ariticulo.ArticuloFiltrosResponse;
import com.vida.apirest.dto.ariticulo.ArticuloParaVentaResponse;
import com.vida.apirest.dto.ariticulo.ArticuloTablaRowResponse;
import com.vida.apirest.dto.ariticulo.ArticuloUpdateRequest;
import com.vida.apirest.dto.ariticulo.VariantCreateRequest;
import com.vida.apirest.dto.ariticulo.VarianteCompactResponse;
import com.vida.apirest.dto.common.PageResponse;
import com.vida.apirest.servicies.ArticuloService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/articulos")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('VER_ARTICULOS')")
public class ArticuloController {

    private final ArticuloService articuloService;

    @GetMapping("/tabla/filtros")
    public ResponseEntity<ArticuloFiltrosResponse> filtrosTabla() {
        return ResponseEntity.ok(articuloService.obtenerFiltrosTabla());
    }

    @GetMapping("/para-venta")
    public ResponseEntity<PageResponse<ArticuloParaVentaResponse>> listParaVenta(
            @RequestParam Long sucursalId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        return ResponseEntity.ok(articuloService.findParaVentaPage(sucursalId, q, page, size));
    }

    @GetMapping("/tabla")
    public ResponseEntity<PageResponse<ArticuloTablaRowResponse>> listTabla(
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String subCategoria,
            @RequestParam(required = false) String genero,
            @RequestParam(required = false) String marca,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long depositoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        return ResponseEntity.ok(articuloService.findTablaPage(
                categoria, subCategoria, genero, marca, q, depositoId, page, size));
    }

    @PostMapping
    public ResponseEntity<ArticuloCompactResponse> createArticulo(@RequestBody ArticuloCreateRequest request) {
        var articulo = articuloService.createArticulo(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                articuloService.getCompactById(articulo.getId()));
    }

    @PutMapping("/{id:[0-9]+}")
    public ResponseEntity<ArticuloCompactResponse> updateArticulo(
            @PathVariable Long id,
            @RequestBody ArticuloUpdateRequest request
    ) {
        articuloService.updateArticulo(id, request);
        return ResponseEntity.ok(articuloService.getCompactById(id));
    }

    @PutMapping("/{articuloId:[0-9]+}/variantes/{varianteId:[0-9]+}")
    public ResponseEntity<VarianteCompactResponse> actualizarVarianteUnica(
            @PathVariable Long articuloId,
            @PathVariable Long varianteId,
            @RequestBody VariantCreateRequest request
    ) {
        return ResponseEntity.ok(
                articuloService.actualizarVarianteUnica(articuloId, varianteId, request));
    }

    @PutMapping("/{id:[0-9]+}/archivar")
    public ResponseEntity<Map<String, String>> softDeleteArticulo(@PathVariable Long id) {
        articuloService.softDeleteArticulo(id);
        return ResponseEntity.ok(Map.of("message", "Artículo archivado correctamente"));
    }

    @DeleteMapping("/{articuloId:[0-9]+}/variantes/{varianteId:[0-9]+}")
    public ResponseEntity<Map<String, String>> softDeleteVariante(
            @PathVariable Long articuloId,
            @PathVariable Long varianteId
    ) {
        articuloService.softDeleteVariante(articuloId, varianteId);
        return ResponseEntity.ok(Map.of("message", "Variante archivada correctamente"));
    }

    @GetMapping("/{id:[0-9]+}/compact")
    public ResponseEntity<ArticuloCompactResponse> getCompactById(@PathVariable Long id) {
        return ResponseEntity.ok(articuloService.getCompactById(id));
    }

    @PostMapping("/{id:[0-9]+}/variantes")
    public ResponseEntity<VarianteCompactResponse> agregarVariante(
            @PathVariable Long id,
            @RequestBody VariantCreateRequest request,
            @RequestParam(required = false) Long depositoId,
            @RequestParam(required = false) Long sucursalId
    ) {
        VarianteCompactResponse variante = articuloService.agregarVariante(
                id, request, depositoId, sucursalId);
        return ResponseEntity.status(HttpStatus.CREATED).body(variante);
    }

    @GetMapping("/archivados")
    public ResponseEntity<List<ArticuloCompactResponse>> getArchivados() {
        return ResponseEntity.ok(articuloService.getArticulosArchivados());
    }

    @PutMapping("/{id:[0-9]+}/restaurar")
    public ResponseEntity<Map<String, String>> restaurarArticulo(@PathVariable Long id) {
        articuloService.restaurarArticulo(id);
        return ResponseEntity.ok(Map.of("message", "Artículo restaurado correctamente"));
    }

    @GetMapping("/{id:[0-9]+}/variantes/archivadas")
    public ResponseEntity<List<VarianteCompactResponse>> getVariantesArchivadas(@PathVariable Long id) {
        return ResponseEntity.ok(articuloService.getVariantesArchivadas(id));
    }

    @PutMapping("/{articuloId:[0-9]+}/variantes/{varianteId:[0-9]+}/restaurar")
    public ResponseEntity<Map<String, String>> restaurarVariante(
            @PathVariable Long articuloId,
            @PathVariable Long varianteId
    ) {
        articuloService.restaurarVariante(articuloId, varianteId);
        return ResponseEntity.ok(Map.of("message", "Variante restaurada correctamente"));
    }
}
