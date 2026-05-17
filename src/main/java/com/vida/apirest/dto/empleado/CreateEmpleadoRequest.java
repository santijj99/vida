package com.vida.apirest.dto.empleado;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class CreateEmpleadoRequest {
    private String nombre;
    private String apellido;
    private String dni;
    private MultipartFile file;
    private Boolean activo;
    private Long usuarioId;
}
