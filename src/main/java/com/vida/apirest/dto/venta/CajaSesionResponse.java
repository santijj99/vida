package com.vida.apirest.dto.venta;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CajaSesionResponse {
    private Long id;
    private Long cuentaId;
    private String cuentaNombre;
    private String cuentaNumero;
    private LocalDateTime fechaApertura;
    private BigDecimal montoApertura;
    private LocalDateTime fechaCierre;
    private BigDecimal totalIngresos;
    private BigDecimal totalEgresos;
    private BigDecimal montoEsperado;
    private BigDecimal montoContado;
    private BigDecimal diferencia;
    private String estado;
    private String abiertoPor;
    private String cerradoPor;
    private String observacionesCierre;
    private int cantidadMovimientos;
}
