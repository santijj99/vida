package com.vida.apirest.dto.cliente;

import lombok.Data;

@Data
public class ContactoResponse {
    private Long id;
    private String nombre;
    private String apellido;
    private String email;
    private String telefono;
}
