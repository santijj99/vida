package com.vida.apirest.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardCuotaPorEstadoResponse {
    private String estado;
    private long cantidad;
    private BigDecimal total;
}
