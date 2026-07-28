package com.vida.apirest.controller;

import com.vida.apirest.dto.sueldo.EmpleadoSueldoConfigRequest;
import com.vida.apirest.dto.sueldo.EmpleadoSueldoConfigResponse;
import com.vida.apirest.dto.sueldo.LiquidacionSueldoAnularPagoRequest;
import com.vida.apirest.dto.sueldo.LiquidacionSueldoCreateRequest;
import com.vida.apirest.dto.sueldo.LiquidacionSueldoItemDiasDescontadosRequest;
import com.vida.apirest.dto.sueldo.LiquidacionSueldoPagoRequest;
import com.vida.apirest.dto.sueldo.LiquidacionSueldoResponse;
import com.vida.apirest.servicies.SueldoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sueldos")
@RequiredArgsConstructor
public class SueldoController {

    private final SueldoService sueldoService;

    @GetMapping("/config")
    @PreAuthorize("hasAuthority('VER_SUELDOS')")
    public ResponseEntity<List<EmpleadoSueldoConfigResponse>> listarConfigs() {
        return ResponseEntity.ok(sueldoService.listarConfigs());
    }

    @PutMapping("/config")
    @PreAuthorize("hasAuthority('GESTIONAR_SUELDOS')")
    public ResponseEntity<EmpleadoSueldoConfigResponse> upsertConfig(@RequestBody EmpleadoSueldoConfigRequest request) {
        return ResponseEntity.ok(sueldoService.upsertConfig(request));
    }

    @GetMapping("/liquidaciones")
    @PreAuthorize("hasAuthority('VER_SUELDOS')")
    public ResponseEntity<List<LiquidacionSueldoResponse>> listar(
            @RequestParam(required = false) Long sucursalId) {
        return ResponseEntity.ok(sueldoService.listarLiquidaciones(sucursalId));
    }

    @GetMapping("/liquidaciones/{id}")
    @PreAuthorize("hasAuthority('VER_SUELDOS')")
    public ResponseEntity<LiquidacionSueldoResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(sueldoService.obtenerLiquidacion(id));
    }

    @PostMapping("/liquidaciones")
    @PreAuthorize("hasAuthority('GESTIONAR_SUELDOS')")
    public ResponseEntity<LiquidacionSueldoResponse> crear(@RequestBody LiquidacionSueldoCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sueldoService.crearLiquidacion(request));
    }

    @PostMapping("/liquidaciones/{id}/pagar")
    @PreAuthorize("hasAuthority('GESTIONAR_SUELDOS')")
    public ResponseEntity<LiquidacionSueldoResponse> pagar(
            @PathVariable Long id,
            @RequestBody LiquidacionSueldoPagoRequest request) {
        return ResponseEntity.ok(sueldoService.pagar(id, request));
    }

    @PostMapping("/liquidaciones/{id}/anular-pago")
    @PreAuthorize("hasAuthority('GESTIONAR_SUELDOS')")
    public ResponseEntity<LiquidacionSueldoResponse> anularPago(
            @PathVariable Long id,
            @RequestBody(required = false) LiquidacionSueldoAnularPagoRequest request) {
        return ResponseEntity.ok(sueldoService.anularPago(id, request != null ? request : new LiquidacionSueldoAnularPagoRequest()));
    }

    @PostMapping("/liquidaciones/{id}/recalcular")
    @PreAuthorize("hasAuthority('GESTIONAR_SUELDOS')")
    public ResponseEntity<LiquidacionSueldoResponse> recalcular(@PathVariable Long id) {
        return ResponseEntity.ok(sueldoService.recalcular(id));
    }

    @PostMapping("/liquidaciones/{id}/cancelar")
    @PreAuthorize("hasAuthority('GESTIONAR_SUELDOS')")
    public ResponseEntity<LiquidacionSueldoResponse> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(sueldoService.cancelar(id));
    }

    @PutMapping("/liquidaciones/{id}/items/{itemId}/dias-descontados")
    @PreAuthorize("hasAuthority('GESTIONAR_SUELDOS')")
    public ResponseEntity<LiquidacionSueldoResponse> actualizarDiasDescontados(
            @PathVariable Long id,
            @PathVariable Long itemId,
            @RequestBody LiquidacionSueldoItemDiasDescontadosRequest request) {
        return ResponseEntity.ok(sueldoService.actualizarDiasDescontados(id, itemId, request));
    }
}
