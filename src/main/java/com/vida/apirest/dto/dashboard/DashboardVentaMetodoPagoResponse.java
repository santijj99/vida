package com.vida.apirest.dto.dashboard;

import java.math.BigDecimal;

public record DashboardVentaMetodoPagoResponse(
        String metodoPago,
        BigDecimal total,
        Long cantidadPagos
) {
}
