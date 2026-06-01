package com.vida.apirest.dto.cliente;

import lombok.Data;

import java.util.List;

@Data
public class ClienteResponse {
    private Long id;
    private String nombre;
    private String apellido;
    private String dni;
    private String telefono;
    private String trabajo;
    private Long direccionId;
    private DireccionResponse direccion;
    private Long garanteId;
    private String garanteNombre;
    private List<ContactoResponse> contactos;
}
