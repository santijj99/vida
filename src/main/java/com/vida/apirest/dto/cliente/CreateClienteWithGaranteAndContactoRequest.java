package com.vida.apirest.dto.cliente;

import lombok.Data;

import java.util.List;

@Data
public class CreateClienteWithGaranteAndContactoRequest {
    private String nombre;
    private String apellido;
    private String dni;
    private Long direccionId;
    private Long garanteId;
    private List<ContactoRequest> contactos;
}
