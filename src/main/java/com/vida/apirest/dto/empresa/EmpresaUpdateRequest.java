package com.vida.apirest.dto.empresa;

import lombok.Data;

@Data
public class EmpresaUpdateRequest {
    private String nombre;
    private String codigo;
    private String cuit;
    private String razonSocial;
    private String domicilio;
    private String ciudad;
    private String provincia;
    private String estado;
}
