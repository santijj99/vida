package com.vida.apirest.dto.finanzas;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MonedaResponse {
    private Long id;
    private String codigo;
    private String nombre;
    private String simbolo;
    private BigDecimal tasaCambio;
    private Integer decimalPlaces;
    private Boolean activo;
    private Boolean predeterminada;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
