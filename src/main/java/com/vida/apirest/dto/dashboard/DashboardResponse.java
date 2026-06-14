package com.vida.apirest.dto.dashboard;

import java.util.List;

public record DashboardResponse(
        List<DashboardClienteTopResponse> topClientes,
        List<DashboardArticuloTopResponse> topArticulos,
        List<DashboardVentaMetodoPagoResponse> ventasPorMetodoPago,
        DashboardValorStockResponse valorStock
) {
}
