package com.vida.apirest.servicies;

import com.vida.apirest.dto.venta.VentaDetalleRequest;
import com.vida.apirest.model.articulo.Articulo;
import com.vida.apirest.model.articulo.VarianteArticulo;
import com.vida.apirest.model.venta.Venta;
import com.vida.apirest.model.venta.VentaDetalle;
import com.vida.apirest.repositories.ArticuloRepository;
import com.vida.apirest.repositories.VarianteArticuloRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class VentaDetalleSupport {

    private final ArticuloRepository articuloRepository;
    private final VarianteArticuloRepository varianteArticuloRepository;
    private final PromocionService promocionService;

    public record ArticuloVarianteResuelto(Articulo articulo, VarianteArticulo variante) {
    }

    public record MontosDetalle(
            BigDecimal subtotal,
            BigDecimal descuentoMonto,
            BigDecimal impuesto,
            BigDecimal total
    ) {
    }

    public ArticuloVarianteResuelto resolverArticuloYVariante(VentaDetalleRequest detalleReq) {
        if (detalleReq.getVarianteId() != null) {
            VarianteArticulo variante = varianteArticuloRepository.findById(detalleReq.getVarianteId())
                    .orElseThrow(() -> new RuntimeException("Variante no encontrada con ID: " + detalleReq.getVarianteId()));
            Articulo articulo = articuloRepository.findById(variante.getArticuloId())
                    .orElseThrow(() -> new RuntimeException(
                            "Artículo de la variante no encontrado con ID: " + variante.getArticuloId()));
            return new ArticuloVarianteResuelto(articulo, variante);
        }

        if (detalleReq.getArticuloId() == null) {
            throw new RuntimeException("Cada detalle requiere articuloId o varianteId");
        }
        Articulo articulo = articuloRepository.findById(detalleReq.getArticuloId())
                .orElseThrow(() -> new RuntimeException("Artículo no encontrado con ID: " + detalleReq.getArticuloId()));
        return new ArticuloVarianteResuelto(articulo, null);
    }

    public void validarCantidad(Integer cantidad) {
        if (cantidad == null || cantidad <= 0) {
            throw new RuntimeException("La cantidad del detalle debe ser mayor a cero");
        }
    }

    public BigDecimal resolverPrecioUnitario(VentaDetalleRequest detalleReq, VarianteArticulo variante) {
        if (variante != null) {
            BigDecimal precio = obtenerPrecioUnitarioDesdeVariante(variante);
            if (precio == null || precio.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("No existe precio unitario válido para la variante con ID: " + variante.getId());
            }
            return precio;
        }
        if (detalleReq.getPrecioUnitario() == null || detalleReq.getPrecioUnitario().compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("El precio unitario del detalle debe ser un valor válido");
        }
        return detalleReq.getPrecioUnitario();
    }

    public MontosDetalle calcularMontos(BigDecimal precioUnitario, VentaDetalleRequest detalleReq) {
        BigDecimal descuentoMonto = detalleReq.getDescuentoMonto() != null ? detalleReq.getDescuentoMonto() : BigDecimal.ZERO;
        BigDecimal impuesto = detalleReq.getImpuesto() != null ? detalleReq.getImpuesto() : BigDecimal.ZERO;
        BigDecimal subtotal = precioUnitario.multiply(BigDecimal.valueOf(detalleReq.getCantidad())).subtract(descuentoMonto);
        return new MontosDetalle(subtotal, descuentoMonto, impuesto, subtotal.add(impuesto));
    }

    public VentaDetalle construirDetalle(
            Venta venta,
            ArticuloVarianteResuelto resolucion,
            VentaDetalleRequest detalleReq,
            BigDecimal precioUnitario,
            MontosDetalle montos
    ) {
        VentaDetalle detalle = new VentaDetalle();
        detalle.setVenta(venta);
        detalle.setArticulo(resolucion.articulo());
        detalle.setVariante(resolucion.variante());
        detalle.setCantidad(detalleReq.getCantidad());
        detalle.setPrecioUnitario(precioUnitario);
        detalle.setDescuentoPorcentaje(
                detalleReq.getDescuentoPorcentaje() != null ? detalleReq.getDescuentoPorcentaje() : BigDecimal.ZERO);
        detalle.setDescuentoMonto(montos.descuentoMonto());
        detalle.setImpuesto(montos.impuesto());
        detalle.setSubtotal(montos.subtotal());
        detalle.setTotal(montos.total());
        detalle.setLote(detalleReq.getLote());
        detalle.setNumeroSerie(detalleReq.getNumeroSerie());
        return detalle;
    }

    public BigDecimal resolverPrecioReferencia(VarianteArticulo variante, boolean aplicarPromocion) {
        if (variante == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal precioLista = obtenerPrecioListaDesdeVariante(variante);
        if (precioLista == null) {
            return BigDecimal.ZERO;
        }
        if (!aplicarPromocion) {
            return precioLista;
        }
        return promocionService.resolverPrecioVenta(variante.getId(), precioLista);
    }

    public BigDecimal obtenerPrecioUnitarioDesdeVariante(VarianteArticulo variante) {
        if (variante == null) {
            return null;
        }
        BigDecimal precioLista = obtenerPrecioListaDesdeVariante(variante);
        if (precioLista == null) {
            return null;
        }
        return promocionService.resolverPrecioVenta(variante.getId(), precioLista);
    }

    private BigDecimal obtenerPrecioListaDesdeVariante(VarianteArticulo variante) {
        if (variante.getHistorialPrecios() != null && !variante.getHistorialPrecios().isEmpty()) {
            return variante.getHistorialPrecios().stream()
                    .max((a, b) -> a.getFecha().compareTo(b.getFecha()))
                    .map(historialPrecio -> historialPrecio.getPrecioNuevo())
                    .orElse(null);
        }
        if (variante.getListaPrecio() != null && variante.getListaPrecio().getPrecio() != null) {
            return variante.getListaPrecio().getPrecio();
        }
        return null;
    }

    public static String descripcionLinea(VentaDetalle detalle) {
        if (detalle == null) {
            return "Artículo";
        }
        StringBuilder sb = new StringBuilder();
        Articulo articulo = detalle.getArticulo();
        if (articulo != null) {
            if (articulo.getModelo() != null && !articulo.getModelo().isBlank()) {
                sb.append(articulo.getModelo());
            } else if (articulo.getDescripcion() != null && !articulo.getDescripcion().isBlank()) {
                sb.append(articulo.getDescripcion());
            } else if (articulo.getCodigo() != null) {
                sb.append(articulo.getCodigo());
            }
        }
        VarianteArticulo variante = detalle.getVariante();
        if (variante != null) {
            if (variante.getTalle() != null && variante.getTalle().getNumero() != null) {
                if (!sb.isEmpty()) {
                    sb.append(' ');
                }
                sb.append("Talle ").append(variante.getTalle().getNumero());
            }
            if (variante.getColor() != null && variante.getColor().getNombre() != null) {
                if (!sb.isEmpty()) {
                    sb.append(' ');
                }
                sb.append(variante.getColor().getNombre());
            }
        }
        return !sb.isEmpty() ? sb.toString() : "Artículo";
    }
}
