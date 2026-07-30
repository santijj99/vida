package com.vida.apirest.dto.ticket;

import lombok.Data;

@Data
public class TicketConfigResponse {
    private Long id;
    private Long empresaId;
    private String empresaNombre;
    private String formato;
    private Boolean abrirAutomaticamente;
}
