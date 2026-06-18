package com.vida.apirest.dto.dashboard;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class DashboardCreditosResumenResponse {
    private long cuentasActivas;
    private long cuentasAlDia;
    private long cuentasConVencidos;
    private BigDecimal saldoTotalCuentas;
    private List<DashboardCuotaPorEstadoResponse> cuotasPorEstado;
    private List<DashboardCuentaCreditoItemResponse> cuentas;
}
