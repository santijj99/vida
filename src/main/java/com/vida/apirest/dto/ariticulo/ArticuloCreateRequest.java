package com.vida.apirest.dto.ariticulo;

import java.util.List;

import lombok.Data;

@Data
public class ArticuloCreateRequest {
    private String marca;
    private String categoria;
    private String subCategoria;
    private List<String> clasificaciones;
    private String genero;
    private String codigo;
    private String modelo;
    private String descripcion;
    private String color;
    private List<VariantCreateRequest> variantes;
    private Long depositoId;   
    private Long sucursalId;  
}