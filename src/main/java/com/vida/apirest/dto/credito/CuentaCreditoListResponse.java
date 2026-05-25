package com.vida.apirest.dto.credito;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CuentaCreditoListResponse {
    private Long id;
    private String numero;
    private Long clienteId;
    private String clienteNombre;
    private String clienteApellido;
    private String clienteDni;
    private Long sucursalId;
    private String sucursalNombre;
    private BigDecimal saldoActual;
    private BigDecimal limiteCredito;
    private Boolean activo;
}
