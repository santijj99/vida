package com.vida.apirest.dto.finanzas;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CuentaFinancieraResponse {
    private Long id;
    private Long sucursalId;
    private Long monedaId;
    private String nombre;
    private String numero;
    private String tipo;
    private String banco;
    private BigDecimal saldoInicial;
    private BigDecimal saldoActual;
    private String personaResponsable;
    private Boolean activo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
