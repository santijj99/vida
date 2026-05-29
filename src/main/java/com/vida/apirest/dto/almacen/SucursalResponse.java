package com.vida.apirest.dto.almacen;

import lombok.Data;

@Data
public class SucursalResponse {
    private Long id;
    private Long empresaId;
    private String empresaNombre;
    private String nombre;
    private String codigo;
    private String domicilio;
    private String ciudad;
    private String provincia;
    private String estado;
}
