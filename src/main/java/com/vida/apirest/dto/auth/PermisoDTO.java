package com.vida.apirest.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PermisoDTO {
    private Long id;
    private String codigo;
    private String nombre;
    private String modulo;
    private String descripcion;
}
