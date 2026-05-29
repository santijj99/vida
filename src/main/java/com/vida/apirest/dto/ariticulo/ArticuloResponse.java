package com.vida.apirest.dto.ariticulo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ArticuloResponse {
    private Long id;
    private String codigo;
    private String marca;
    private String modelo;
    private String descripcion;
    private String categoria;
    private String subCategoria;
    private String genero;
    private BigDecimal precio;
    private BigDecimal precioCompra;
    private String estado;
    private List<VarianteCompactResponse> variantes;
    private Integer cantidad;
}
