package com.vida.apirest.dto.almacen;

import lombok.Data;

@Data
public class DepositoResponse {
    private Long id;
    private Long sucursalId;
    private String sucursalNombre;
    private String nombre;
    private String codigo;
    private String ubicacion;
    private String descripcion;
    private String tipo;
}
