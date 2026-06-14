package com.vida.apirest.dto.pedido;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrdenCompraVarianteLookupResponse {
    private Long varianteId;
    private Long articuloId;
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
    private BigDecimal precioReferencia;
    private BigDecimal precioCosto;
    private Boolean enSistema;
    private Boolean catalogoActivo;
}
