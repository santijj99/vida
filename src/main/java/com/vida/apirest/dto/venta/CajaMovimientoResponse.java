package com.vida.apirest.dto.venta;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CajaMovimientoResponse {
    private Long id;
    private Long cuentaId;
    private String cuentaNombre;
    private String numero;
    private String tipo;
    private BigDecimal monto;
    private BigDecimal saldoAnterior;
    private BigDecimal saldoNuevo;
    private String descripcion;
    private String referencia;
    private String responsable;
    private LocalDateTime createdAt;
}
