package com.vida.apirest.dto.venta;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class VentaDetalleResponse {
    private Long id;
    private Long articuloId;
    private Long varianteId;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal descuentoPorcentaje;
    private BigDecimal descuentoMonto;
    private BigDecimal subtotal;
    private BigDecimal impuesto;
    private BigDecimal total;
    private String lote;
    private String numeroSerie;
    private String codigoArticulo;
    private String descripcionArticulo;
    private String talle;
    private String color;
}
