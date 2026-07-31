package com.vida.apirest.dto.credito;

import lombok.Data;

@Data
public class QuitarRecargoRequest {
    /** Motivo obligatorio de la exención del recargo. */
    private String motivo;
}
