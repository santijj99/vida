package com.vida.apirest.dto.empresa;

import lombok.Data;

@Data
public class EmpresaResponse {
    private Long id;
    private String nombre;
    private String codigo;
    private String cuit;
    private String razonSocial;
    private String domicilio;
    private String ciudad;
    private String estado;
}
