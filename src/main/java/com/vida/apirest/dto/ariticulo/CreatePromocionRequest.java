package com.vida.apirest.dto.ariticulo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CreatePromocionRequest {
    private String nombre;
    private String descripcion;
    private BigDecimal porcentajeDescuento;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private Boolean activo;
    private List<PromocionVarianteRequest> variantes;
}
