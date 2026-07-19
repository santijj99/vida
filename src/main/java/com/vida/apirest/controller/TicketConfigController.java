package com.vida.apirest.controller;

import com.vida.apirest.dto.ticket.TicketConfigRequest;
import com.vida.apirest.dto.ticket.TicketConfigResponse;
import com.vida.apirest.servicies.TicketConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets/config")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('CONFIGURAR_TICKETS')")
public class TicketConfigController {

    private final TicketConfigService ticketConfigService;

    @GetMapping
    public ResponseEntity<TicketConfigResponse> obtener(
            @RequestParam(required = false) Long empresaId) {
        return ResponseEntity.ok(ticketConfigService.obtener(empresaId));
    }

    @PutMapping
    public ResponseEntity<TicketConfigResponse> guardar(@RequestBody TicketConfigRequest request) {
        return ResponseEntity.ok(ticketConfigService.guardar(request));
    }
}
