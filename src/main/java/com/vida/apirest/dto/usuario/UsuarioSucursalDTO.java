package com.vida.apirest.dto.usuario;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioSucursalDTO {
    private Long id;
    private String nombre;
    private String codigo;
    private String estado;
}
