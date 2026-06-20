package com.vida.apirest.servicies;

import com.vida.apirest.model.almacen.Stock;
import com.vida.apirest.model.articulo.Articulo;
import com.vida.apirest.model.articulo.VarianteArticulo;
import com.vida.apirest.repositories.ArticuloRepository;
import com.vida.apirest.repositories.VarianteArticuloRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class PendienteDetalleResolver {

    private final ArticuloRepository articuloRepository;
    private final VarianteArticuloRepository varianteArticuloRepository;
    private final StockOperacionesService stockOperacionesService;
    private final VentaDetalleSupport ventaDetalleSupport;

    public record DetalleResuelto(Articulo articulo, VarianteArticulo variante, Stock stock, BigDecimal precio) {
    }

    public DetalleResuelto resolver(
            Long articuloId,
            Long varianteId,
            Integer cantidad,
            Long sucursalId,
            boolean aplicarPromocion
    ) {
        ventaDetalleSupport.validarCantidad(cantidad);

        VarianteArticulo variante = null;
        Articulo articulo;

        if (varianteId != null) {
            variante = varianteArticuloRepository.findById(varianteId)
                    .orElseThrow(() -> new RuntimeException("Variante no encontrada"));
            articulo = articuloRepository.findById(variante.getArticuloId())
                    .orElseThrow(() -> new RuntimeException("Artículo no encontrado"));
        } else {
            if (articuloId == null) {
                throw new RuntimeException("Cada detalle requiere articuloId o varianteId");
            }
            articulo = articuloRepository.findById(articuloId)
                    .orElseThrow(() -> new RuntimeException("Artículo no encontrado"));
        }

        BigDecimal precio = ventaDetalleSupport.resolverPrecioReferencia(variante, aplicarPromocion);
        Stock stock = obtenerStock(articulo.getId(), variante != null ? variante.getId() : null, sucursalId);
        return new DetalleResuelto(articulo, variante, stock, precio);
    }

    public Stock obtenerStock(Long articuloId, Long varianteId, Long sucursalId) {
        if (varianteId != null) {
            return stockOperacionesService.requireStockByVariante(varianteId, sucursalId);
        }
        return stockOperacionesService.requireStock(articuloId, null, sucursalId);
    }
}
