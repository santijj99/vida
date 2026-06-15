package com.vida.apirest.dto.ariticulo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PromocionResponse {
    private Long id;
    private String nombre;
    private String descripcion;
    private BigDecimal porcentajeDescuento;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private Boolean activo;
    private int cantidadVariantes;
    private LocalDateTime createdAt;
    private List<PromocionVarianteResponse> variantes;
}
