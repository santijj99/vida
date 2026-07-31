package com.vida.apirest.dto.credito;

import lombok.Data;

@Data
public class CancelarCreditoRequest {
    /** Motivo obligatorio de la cancelación. */
    private String motivo;
}
