package com.vida.apirest.dto.venta;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class VentaCambioArticuloResponse {
    private Long id;
    private Long ventaId;
    private Long ventaDetalleId;
    private Long varianteDevueltaId;
    private Long varianteNuevaId;
    private Integer cantidad;
    private String motivo;
    private BigDecimal precioAnterior;
    private BigDecimal precioNuevo;
    private BigDecimal diferenciaPrecio;
    private String observaciones;
    private LocalDateTime createdAt;
}
