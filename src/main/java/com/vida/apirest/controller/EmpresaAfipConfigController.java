package com.vida.apirest.controller;

import com.vida.apirest.dto.empresa.EmpresaAfipConfigRequest;
import com.vida.apirest.dto.empresa.EmpresaAfipConfigResponse;
import com.vida.apirest.servicies.EmpresaAfipConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping(value = "/certificados", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EmpresaAfipConfigResponse> subirCertificados(
            @PathVariable Long empresaId,
            @RequestParam(value = "certificado", required = false) MultipartFile certificado,
            @RequestParam(value = "clavePrivada", required = false) MultipartFile clavePrivada,
            @RequestParam(value = "pkcs12", required = false) MultipartFile pkcs12,
            @RequestParam(value = "clavePrivadaPassword", required = false) String clavePrivadaPassword) {
        return ResponseEntity.ok(empresaAfipConfigService.subirCertificados(
                empresaId, certificado, clavePrivada, pkcs12, clavePrivadaPassword));
    }
}
