package com.vida.apirest.servicies;

import com.vida.apirest.dto.dashboard.DashboardCreditosResumenResponse;
import com.vida.apirest.dto.dashboard.DashboardResponse;
import com.vida.apirest.repositories.DashboardQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int TOP_LIMIT = 5;

    private final DashboardQueryRepository dashboardQueryRepository;
    private final CreditoCuentaService creditoCuentaService;

    @Transactional(readOnly = true)
    public DashboardResponse obtenerDashboard(Long sucursalId) {
        DashboardCreditosResumenResponse creditos = creditoCuentaService.resumenParaDashboard();
        return new DashboardResponse(
                dashboardQueryRepository.topClientes(sucursalId, TOP_LIMIT),
                dashboardQueryRepository.topArticulos(sucursalId, TOP_LIMIT),
                dashboardQueryRepository.ventasPorMetodoPago(sucursalId),
                dashboardQueryRepository.valorStock(sucursalId),
                creditos
        );
    }
}
