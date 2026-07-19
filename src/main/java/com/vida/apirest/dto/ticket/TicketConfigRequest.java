package com.vida.apirest.dto.ticket;

import lombok.Data;

@Data
public class TicketConfigRequest {
    private Long empresaId;
    /** TERMICO_80MM | A4 */
    private String formato;
}
