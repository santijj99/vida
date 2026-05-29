package com.vida.apirest.dto.almacen;

import lombok.Data;

@Data
public class SucursalCreateRequest {
    private Long empresaId;
    private String nombre;
    private String codigo;
    private String domicilio;
    private String ciudad;
    private String provincia;
}