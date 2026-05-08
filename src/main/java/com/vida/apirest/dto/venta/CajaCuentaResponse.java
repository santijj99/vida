package com.vida.apirest.dto.venta;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CajaCuentaResponse {
    private Long id;
    private String nombre;
    private String numero;
    private String tipo;
    private BigDecimal saldoActual;
}
