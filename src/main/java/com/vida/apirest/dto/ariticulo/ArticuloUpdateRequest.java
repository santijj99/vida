package com.vida.apirest.dto.ariticulo;

import java.util.List;

import lombok.Data;

@Data
public class ArticuloUpdateRequest {
    private String marca;
    private String categoria;
    private String subCategoria;
    private String genero;
    private String codigo;
    private String modelo;
    private String descripcion;
    private List<VarianteUpdateRequest> variantes; 
}