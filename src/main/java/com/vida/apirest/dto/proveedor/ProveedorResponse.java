package com.vida.apirest.dto.proveedor;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProveedorResponse {
    private Long id;
    private String codigo;
    private String razonSocial;
    private String nombre;
    private String cuitCuil;
    private String domicilio;
    private String ciudad;
    private String provincia;
    private String pais;
    private String telefono;
    private String email;
    private Boolean activo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
