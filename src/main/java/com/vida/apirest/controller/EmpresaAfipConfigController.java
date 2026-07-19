package com.vida.apirest.controller;

import com.vida.apirest.dto.empresa.EmpresaAfipConfigRequest;
import com.vida.apirest.dto.empresa.EmpresaAfipConfigResponse;
import com.vida.apirest.servicies.EmpresaAfipConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/empresas/{empresaId}/afip-config")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('VER_ORGANIZACION')")
public class EmpresaAfipConfigController {

    private final EmpresaAfipConfigService empresaAfipConfigService;

    @GetMapping
    public ResponseEntity<EmpresaAfipConfigResponse> obtener(@PathVariable Long empresaId) {
        return ResponseEntity.ok(empresaAfipConfigService.obtener(empresaId));
    }

    @PutMapping
    public ResponseEntity<EmpresaAfipConfigResponse> guardar(
            @PathVariable Long empresaId,
            @RequestBody EmpresaAfipConfigRequest request) {
        return ResponseEntity.ok(empresaAfipConfigService.guardar(empresaId, request));
    }
}
