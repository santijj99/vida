package com.vida.apirest.dto.afip;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AfipAmbienteResponse {
    private boolean homologacion;
    private String ambiente;
    private String wsaaUrl;
    private String wsfeUrl;
    private String certificadosDir;
    private String phpScriptPath;
    private String mensaje;
}
