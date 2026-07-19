package com.vida.apirest.dto.credito;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreditoConfigResponse {
    private Long id;
    private Long empresaId;
    private String empresaNombre;
    private Integer diasGracia;
    private BigDecimal porcentajeMora;
    private String tipoInteres;
    private String modoDiaVencimiento;
}
