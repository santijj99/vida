package com.vida.apirest.dto.ariticulo;

import lombok.Data;

import java.util.List;

@Data
public class ArticuloCompactResponse {
    private Long id;
    private String codigo;
    private String modelo;
    private String descripcion;
    private String categoria;
    private String subCategoria;
    private String genero;
    private String marca;
    private List<VarianteCompactResponse> variantes;
}
