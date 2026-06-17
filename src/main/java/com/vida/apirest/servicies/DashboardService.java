package com.vida.apirest.servicies;

import com.vida.apirest.dto.dashboard.DashboardCreditosResumenResponse;
import com.vida.apirest.dto.dashboard.DashboardResponse;
import com.vida.apirest.repositories.DashboardQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private static final int TOP_LIMIT = 5;

    private final DashboardQueryRepository dashboardQueryRepository;
    private final CreditoCuentaService creditoCuentaService;

    @Transactional(readOnly = true)
    public DashboardResponse obtenerDashboard(Long sucursalId) {
        long t0 = System.currentTimeMillis();
        log.info("[Dashboard] GET /api/dashboard — sucursalId={}", sucursalId);

        DashboardCreditosResumenResponse creditos;
        try {
            creditos = creditoCuentaService.resumenParaDashboard();
        } catch (Exception e) {
            log.error("[Dashboard] Falló resumen de créditos", e);
            throw e;
        }

        var response = new DashboardResponse(
                dashboardQueryRepository.topClientes(sucursalId, TOP_LIMIT),
                dashboardQueryRepository.topArticulos(sucursalId, TOP_LIMIT),
                dashboardQueryRepository.ventasPorMetodoPago(sucursalId),
                dashboardQueryRepository.valorStock(sucursalId),
                creditos
        );

        log.info("[Dashboard] Respuesta armada en {} ms", System.currentTimeMillis() - t0);
        return response;
    }
}
