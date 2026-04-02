package com.vida.apirest.dto.cliente;

import lombok.Data;

@Data
public class ContactoRequest {
    private String nombre;
    private String apellido;
    private String email;
    private String telefono;
}
