package com.vida.apirest.dto.empleado;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class CreateEmpleadoRequest {
    private String nombre;
    private String apellido;
    private String dni;
    @JsonIgnore
    private MultipartFile file;
    private Boolean activo;
    private Long usuarioId;
}
