package com.vida.apirest.dto.afip;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReceptorAfipConsultaResponse {
    private boolean encontrado;
    private String razonSocial;
    private String domicilio;
    private Integer condicionIVAReceptorId;
    private String fuente;
    private String mensaje;
}
