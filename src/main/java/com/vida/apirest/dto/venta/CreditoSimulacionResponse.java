package com.vida.apirest.dto.venta;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreditoSimulacionResponse {
    private BigDecimal montoSubtotal;
    private BigDecimal montoInteres;
    private BigDecimal montoTotal;
    private BigDecimal montoAnticipo;
    private BigDecimal montoFinanciado;
    private Integer plazoMeses;
    private BigDecimal tasaInteres;
    private String modoDistribucion;
    private String resumen;
    private List<CreditoCuotaPreviewResponse> cuotas;
}
