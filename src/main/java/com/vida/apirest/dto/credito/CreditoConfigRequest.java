package com.vida.apirest.dto.credito;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreditoConfigRequest {
    private Long empresaId;
    private Integer diasGracia;
    private BigDecimal porcentajeMora;
    private String tipoInteres;
    private String modoDiaVencimiento;
    /** true = recalcular créditos pendientes; false = solo nuevos */
    private Boolean recalcularPendientes;
}
