package com.vida.apirest.dto.finanzas;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TipoCambioResponse {
    private Long id;
    private Long monedaId;
    private String monedaCodigo;
    private String monedaNombre;
    private LocalDate fecha;
    private BigDecimal tasaCompra;
    private BigDecimal tasaVenta;
    private BigDecimal tasaPromedio;
    private String fuente;
    private String observaciones;
    private LocalDateTime createdAt;
    private String usuario;
}
