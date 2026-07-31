package com.vida.apirest.dto.afip;

import lombok.Data;

@Data
public class AfipAmbienteRequest {
    private Boolean homologacion;
    /** Empresa a la que se aplica el ambiente (producción/homologación). */
    private Long empresaId;
}
