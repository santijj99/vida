package com.vida.apirest.dto.pedido;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrdenCompraDetalleResponse {
    private Long id;
    private Long articuloId;
    private Long varianteId;
    private String codigoArticulo;
    private String marca;
    private String categoria;
    private String subCategoria;
    private String genero;
    private String modelo;
    private String paisTalle;
    private String color;
    private String talle;
    private String codigoBarras;
    private Integer cantidadSolicitada;
    private Integer cantidadRecibida;
    private BigDecimal precioUnitario;
    private BigDecimal margenPorcentaje;
    private BigDecimal precioVenta;
    private BigDecimal descuentoPorcentaje;
    private BigDecimal subtotal;
    private String observaciones;
    private Boolean itemEnSistema;
    private Boolean catalogoActivo;
}
