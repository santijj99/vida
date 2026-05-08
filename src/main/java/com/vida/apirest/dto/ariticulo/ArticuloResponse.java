package com.vida.apirest.dto.ariticulo;

import com.vida.apirest.model.articulo.Categoria;
import com.vida.apirest.model.articulo.Color;
import com.vida.apirest.model.articulo.SubCategoria;
import com.vida.apirest.model.articulo.Talle;

import java.math.BigDecimal;
// aqui se coloca lo que lleva para la carga de un articulo
public class ArticuloResponse {
    private Long id;
    private Long varianteId;
    private String codigo;
    private String marca;
    private String modelo;
    private Color color;
    private Talle talle;
    private Categoria categoria;
    private SubCategoria subCategoria;
    private BigDecimal precio;
    private BigDecimal precioCompra;
    private String estado;
    private String variantes;
    private Integer cantidad;
}
