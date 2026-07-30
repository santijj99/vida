package com.vida.apirest.dto.sueldo;

import com.vida.apirest.model.sueldo.LiquidacionSueldo;
import com.vida.apirest.model.sueldo.LiquidacionSueldoItem;
import com.vida.apirest.model.sueldo.PeriodoSueldo;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class LiquidacionSueldoResponse {
    private Long id;
    private String numero;
    private Long sucursalId;
    private String sucursalNombre;
    private PeriodoSueldo periodoTipo;
    private LocalDate fechaDesde;
    private LocalDate fechaHasta;
    private BigDecimal porcentajeComisionOverride;
    private LiquidacionSueldo.EstadoLiquidacion estado;
    private BigDecimal totalSueldos;
    private BigDecimal totalComisiones;
    private BigDecimal totalGeneral;
    private String responsable;
    private String observaciones;
    private LocalDateTime createdAt;
    private List<Item> items;

    @Data
    @Builder
    public static class Item {
        private Long id;
        private Long empleadoId;
        private String empleadoNombre;
        private BigDecimal sueldoBase;
        /** Días no trabajados descontados del fijo. */
        private Integer diasDescontados;
        /** Días laborables del período (antes de descontar), si aplica. */
        private Integer diasLaborablesPeriodo;
        private BigDecimal ventasTotal;
        private Integer cantidadVentas;
        private Integer cantidadArticulos;
        private BigDecimal porcentajeComision;
        private BigDecimal comisionMonto;
        private BigDecimal total;
        private LiquidacionSueldoItem.EstadoItem estado;
        private Long cuentaPagoId;
        private String cuentaPagoNombre;
        private Long movimientoId;
        private LocalDateTime fechaPago;
    }
}
