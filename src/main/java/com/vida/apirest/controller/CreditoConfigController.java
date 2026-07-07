package com.vida.apirest.controller;

import com.vida.apirest.dto.credito.CreditoConfigRequest;
import com.vida.apirest.dto.credito.CreditoConfigResponse;
import com.vida.apirest.servicies.CreditoConfigService;
import com.vida.apirest.servicies.CreditoRecargoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/creditos/config")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('CONFIGURAR_CREDITOS')")
public class CreditoConfigController {

    private final CreditoConfigService configService;
    private final CreditoRecargoService recargoService;

    @GetMapping
    public ResponseEntity<CreditoConfigResponse> obtener(
            @RequestParam(required = false) Long empresaId) {
        return ResponseEntity.ok(configService.obtener(empresaId));
    }

    @PutMapping
    public ResponseEntity<CreditoConfigResponse> guardar(@RequestBody CreditoConfigRequest request) {
        CreditoConfigResponse response = configService.guardar(request);
        if (Boolean.TRUE.equals(request.getRecalcularPendientes()) && response.getEmpresaId() != null) {
            recargoService.recalcularRecargosPendientesEmpresa(response.getEmpresaId());
        }
        return ResponseEntity.ok(response);
    }
}
