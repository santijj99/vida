package com.vida.apirest.dto.sueldo;

import com.vida.apirest.model.sueldo.PeriodoSueldo;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EmpleadoSueldoConfigRequest {
    private Long empleadoId;
    private BigDecimal sueldoFijo;
    private PeriodoSueldo periodoBase;
    private BigDecimal porcentajeComision;
    private Boolean activo;
    private String observaciones;
}
