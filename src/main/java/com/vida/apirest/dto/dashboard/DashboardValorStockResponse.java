package com.vida.apirest.dto.dashboard;

import java.math.BigDecimal;

public record DashboardValorStockResponse(
        Long unidadesDisponibles,
        BigDecimal valorCompra,
        BigDecimal valorVenta
) {
}
