package com.vida.apirest.dto.finanzas;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateTipoCambioRequest {
    private Long monedaId;
    private LocalDate fecha;
    private BigDecimal tasaCompra;
    private BigDecimal tasaVenta;
    private BigDecimal tasaPromedio;
    private String fuente;
    private String observaciones;
}
