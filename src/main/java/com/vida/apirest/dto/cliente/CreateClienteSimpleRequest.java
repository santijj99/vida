package com.vida.apirest.dto.cliente;

import lombok.Data;

@Data
public class CreateClienteSimpleRequest {
    private String nombre;
    private String apellido;
    private String dni;
    private Long direccionId;
}
