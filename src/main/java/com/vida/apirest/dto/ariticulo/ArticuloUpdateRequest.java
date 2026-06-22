package com.vida.apirest.dto.ariticulo;

import java.util.List;

import lombok.Data;

@Data
public class ArticuloUpdateRequest {
    private String marca;
    private String categoria;
    /** @deprecated usar {@link #clasificaciones} */
    private String subCategoria;
    private List<String> clasificaciones;
    private String genero;
    private String codigo;
    private String modelo;
    private String descripcion;
    private List<VarianteUpdateRequest> variantes; 
}