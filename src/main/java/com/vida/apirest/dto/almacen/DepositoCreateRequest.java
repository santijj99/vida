package com.vida.apirest.dto.almacen;

import lombok.Data;

@Data
public class DepositoCreateRequest {
    private Long sucursalId;
    private String nombre;
    private String codigo;
    private String ubicacion;
    private String descripcion;
    private String tipo;
}