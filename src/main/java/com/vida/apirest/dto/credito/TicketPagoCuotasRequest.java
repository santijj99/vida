package com.vida.apirest.dto.credito;

import lombok.Data;

import java.util.List;

@Data
public class TicketPagoCuotasRequest {
    private List<Long> pagoIds;
}
