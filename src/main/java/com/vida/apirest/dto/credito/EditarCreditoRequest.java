package com.vida.apirest.dto.credito;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class EditarCreditoRequest {
    private Integer plazoMeses;
    private LocalDateTime fechaPrimerVencimiento;
    private String descripcion;
    private List<CuotaEdicionRequest> cuotas;
}
