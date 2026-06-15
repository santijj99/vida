package com.vida.apirest.dto.dashboard;

import java.math.BigDecimal;

public record DashboardArticuloTopResponse(
        Long articuloId,
        String codigo,
        String marca,
        String modelo,
        Long cantidadVendida,
        BigDecimal importeTotal
) {
}
