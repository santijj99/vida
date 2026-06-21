package com.vida.apirest.dto.ariticulo;

import lombok.Data;

@Data
public class CreateClasificacionRequest {
    private String nombre;
    private String descripcion;
    private Boolean activo;
}
