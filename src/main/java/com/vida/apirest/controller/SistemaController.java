package com.vida.apirest.controller;

import com.vida.apirest.dto.sistema.SistemaInfoResponse;
import com.vida.apirest.servicies.licencia.SistemaLicenciaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sistema")
@RequiredArgsConstructor
public class SistemaController {

    private final SistemaLicenciaService sistemaLicenciaService;

    @GetMapping("/info")
    public ResponseEntity<SistemaInfoResponse> info(
            @RequestParam(defaultValue = "false") boolean refresh) {
        return ResponseEntity.ok(sistemaLicenciaService.obtenerInfo(refresh));
    }

    @PostMapping("/licencia/validar")
    public ResponseEntity<SistemaInfoResponse> validarAhora() {
        return ResponseEntity.ok(sistemaLicenciaService.obtenerInfo(true));
    }
}
