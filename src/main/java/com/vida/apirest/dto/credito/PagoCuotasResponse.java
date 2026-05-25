package com.vida.apirest.dto.credito;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PagoCuotasResponse {
    private BigDecimal totalCuotas;
    private BigDecimal montoEntregado;
    private BigDecimal montoAplicado;
    private BigDecimal cambio;
    private List<CuotaCreditoResponse> cuotasActualizadas;
}
