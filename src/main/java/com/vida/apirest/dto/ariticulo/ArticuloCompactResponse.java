package com.vida.apirest.dto.ariticulo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ArticuloCompactResponse {
    private Long id;
    private String codigo;
    private String modelo;
    private String descripcion;
    private String categoria;
    /** Primera clasificación (compatibilidad). */
    private String subCategoria;
    private List<String> clasificaciones = new ArrayList<>();
    private String genero;
    private String marca;
    private List<VarianteCompactResponse> variantes;
}
