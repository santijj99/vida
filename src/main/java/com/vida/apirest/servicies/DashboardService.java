package com.vida.apirest.servicies;

import com.vida.apirest.dto.dashboard.DashboardCreditosResumenResponse;
import com.vida.apirest.dto.dashboard.DashboardResponse;
import com.vida.apirest.repositories.DashboardQueryRepository;
import com.vida.apirest.security.SucursalScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int TOP_LIMIT = 5;

    private final DashboardQueryRepository dashboardQueryRepository;
    private final CreditoCuentaService creditoCuentaService;
    private final SucursalScopeService sucursalScopeService;

    @Transactional(readOnly = true)
    public DashboardResponse obtenerDashboard(Long sucursalId) {
        Long scopedSucursalId = sucursalScopeService.enforceFilter(sucursalId);
        DashboardCreditosResumenResponse creditos = creditoCuentaService.resumenParaDashboard();
        return new DashboardResponse(
                dashboardQueryRepository.topClientes(scopedSucursalId, TOP_LIMIT),
                dashboardQueryRepository.topArticulos(scopedSucursalId, TOP_LIMIT),
                dashboardQueryRepository.ventasPorMetodoPago(scopedSucursalId),
                dashboardQueryRepository.valorStock(scopedSucursalId),
                creditos
        );
    }
}
