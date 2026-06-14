package com.vida.apirest.dto.dashboard;

import java.math.BigDecimal;

public record DashboardClienteTopResponse(
        Long clienteId,
        String nombre,
        String apellido,
        String dni,
        BigDecimal totalPagado,
        Long cantidadVentas
) {
}
