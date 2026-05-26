package com.vida.apirest.dto.prestamo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PrestamoCondicionalDetalleResponse {
    private Long id;
    private Long articuloId;
    private Long varianteId;
    private String codigo;
    private String descripcion;
    private String talle;
    private String color;
    private Integer cantidad;
    private BigDecimal precioUnitarioReferencia;
    private BigDecimal subtotalReferencia;
    private String estado;
}
