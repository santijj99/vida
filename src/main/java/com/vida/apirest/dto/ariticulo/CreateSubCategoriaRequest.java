package com.vida.apirest.dto.ariticulo;

import lombok.Data;

@Data
public class CreateSubCategoriaRequest {
    private String nombre;
    private String descripcion;
    private Boolean activo;
}
