package com.vida.apirest.dto.finanzas;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GastoCategoriaRequest {
    private String nombre;
    private String codigo;
    private String descripcion;
    private Boolean activo;
}
