package com.vida.apirest.dto.venta;

import lombok.Data;

@Data
public class VentaCambioArticuloRequest {
    private Long ventaDetalleId;
    private Long nuevaVarianteId;
    private Integer cantidad;
    private String motivo;
    private String observaciones;
}
