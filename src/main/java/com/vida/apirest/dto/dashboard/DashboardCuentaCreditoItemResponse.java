package com.vida.apirest.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardCuentaCreditoItemResponse {
    private Long cuentaId;
    private String numero;
    private String clienteNombre;
    private String clienteDni;
    private BigDecimal saldoActual;
    private boolean tieneCreditosVencidos;
    private int cantidadCreditos;
}
