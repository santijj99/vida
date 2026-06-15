package com.vida.apirest.dto.pedido;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrdenCompraDetalleRequest {
    private Long varianteId;
    private String codigoArticulo;
    private String codigoBarras;
    private String marca;
    private String categoria;
    private String subCategoria;
    private String genero;
    private String modelo;
    private String paisTalle;
    private String color;
    private String talle;
    private Integer cantidadSolicitada;
    private BigDecimal precioUnitario;
    private BigDecimal margenPorcentaje;
    private BigDecimal precioVenta;
    private BigDecimal descuentoPorcentaje;
    private String observaciones;
}
