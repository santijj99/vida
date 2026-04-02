package com.vida.apirest.dto.cliente;

import lombok.Data;

import java.util.List;

@Data
public class CreateClienteRequest {
    private String nombre;
    private String apellido;
    private String dni;
    private String image;
    private Long garanteId;
    private List<ContactoRequest> contactos;
}
