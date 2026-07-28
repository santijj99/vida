package com.vida.apirest.dto.sueldo;

import com.vida.apirest.model.sueldo.PeriodoSueldo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class EmpleadoSueldoConfigRequest {
    private Long empleadoId;
    private BigDecimal sueldoFijo;
    private PeriodoSueldo periodoBase;
    private BigDecimal porcentajeComision;
    /** ISO 1=lun … 7=dom. Aplica cuando periodoBase=DIA. */
    private List<Integer> diasLaborables;
    private Boolean activo;
    private String observaciones;
}
