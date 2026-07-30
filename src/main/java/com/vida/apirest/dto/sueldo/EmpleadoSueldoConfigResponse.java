package com.vida.apirest.dto.sueldo;

import com.vida.apirest.model.sueldo.PeriodoSueldo;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class EmpleadoSueldoConfigResponse {
    private Long id;
    private Long empleadoId;
    private String empleadoNombre;
    private String roles;
    private BigDecimal sueldoFijo;
    private PeriodoSueldo periodoBase;
    private BigDecimal porcentajeComision;
    /** ISO 1=lun … 7=dom. */
    private List<Integer> diasLaborables;
    private Boolean activo;
    private String observaciones;
    private LocalDateTime updatedAt;
}
