package com.vida.apirest.controller;

import com.vida.apirest.dto.dashboard.DashboardResponse;
import com.vida.apirest.servicies.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @PreAuthorize("hasAuthority('VER_DASHBOARD')")
    public ResponseEntity<DashboardResponse> obtener(
            @RequestParam(required = false) Long sucursalId
    ) {
        return ResponseEntity.ok(dashboardService.obtenerDashboard(sucursalId));
    }
}
