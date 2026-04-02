package com.vida.apirest.dto.empleado;

import lombok.Data;

@Data
public class EmpleadoResponse {
    private Long id;
    private String nombre;
    private String apellido;
    private String dni;
    private String image;
    private Boolean activo;
    private Long usuarioId;
}
