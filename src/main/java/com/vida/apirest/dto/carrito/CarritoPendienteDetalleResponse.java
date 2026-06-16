package com.vida.apirest.dto.carrito;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CarritoPendienteDetalleResponse {
    private Long id;
    private Long articuloId;
    private Long varianteId;
    private String codigo;
    private String descripcion;
    private String talle;
    private String color;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
}
