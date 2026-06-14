package com.vida.apirest.dto.proveedor;

import lombok.Data;

@Data
public class ProveedorRequest {
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
}
