package com.vida.apirest.controller;

import com.vida.apirest.dto.dashboard.DashboardResponse;
import com.vida.apirest.servicies.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @PreAuthorize("hasAuthority('VER_DASHBOARD')")
    public ResponseEntity<?> obtener(
            @RequestParam(required = false) Long sucursalId
    ) {
        try {
            DashboardResponse response = dashboardService.obtenerDashboard(sucursalId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[Dashboard] Error en GET /api/dashboard sucursalId={}: {}", sucursalId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "message", e.getMessage() != null ? e.getMessage() : "Error interno en dashboard",
                    "statusCode", HttpStatus.INTERNAL_SERVER_ERROR.value()
            ));
        }
    }
}
