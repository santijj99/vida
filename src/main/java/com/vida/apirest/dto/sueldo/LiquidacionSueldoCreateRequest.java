package com.vida.apirest.dto.sueldo;

import com.vida.apirest.model.sueldo.PeriodoSueldo;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class LiquidacionSueldoCreateRequest {
    private Long sucursalId;
    private PeriodoSueldo periodoTipo;
    private LocalDate fechaDesde;
    private LocalDate fechaHasta;
    /** Opcional: aplica a todos los ítems de esta liquidación. */
    private BigDecimal porcentajeComisionOverride;
    private String observaciones;
    /** Si vacío, incluye todos los empleados activos con config activa. */
    private List<Long> empleadoIds;
    /** Si true, ignora la validación de solapamiento con otras liquidaciones activas. */
    private Boolean permitirSolapamiento;
    /**
     * Si true (default), prorratea el sueldo fijo según los días del rango.
     * Si false, paga el sueldo fijo configurado completo, sin prorratear.
     */
    private Boolean prorratearSueldo;
}
