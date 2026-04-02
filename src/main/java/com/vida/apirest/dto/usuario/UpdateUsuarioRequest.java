package com.vida.apirest.dto.usuario;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UpdateUsuarioRequest {
    private String nombre;
    private String apellido;
    private String celular;
    private MultipartFile file;
}
