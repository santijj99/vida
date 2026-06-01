package com.vida.apirest.dto.afip;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class EmitirFacturaAFIPRequest {
    private Integer cbteTipo;
    private Integer docTipo;
    private String docNro;
    private String razonSocial;
    private Integer condicionIVAReceptorId;
    private String domicilio;
    private BigDecimal montoAFacturar;
}
