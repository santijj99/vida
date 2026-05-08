package com.vida.apirest.dto.venta;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class VentaDetalleRequest {
    private Long articuloId;
    private Long varianteId;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal descuentoPorcentaje;
    private BigDecimal descuentoMonto;
    private BigDecimal impuesto;
    private String lote;
    private String numeroSerie;
}
