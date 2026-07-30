package com.vida.apirest.model.sueldo;

import com.vida.apirest.model.finanzas.CuentaFinanciera;
import com.vida.apirest.model.persona.Empleado;
import com.vida.apirest.model.tesoreria.MovimientoFinanciero;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "liquidacion_sueldo_item",
        indexes = {
                @Index(name = "ix_liq_item_liquidacion", columnList = "liquidacion_id"),
                @Index(name = "ix_liq_item_empleado", columnList = "empleado_id"),
                @Index(name = "ix_liq_item_estado", columnList = "estado")
        }
)
public class LiquidacionSueldoItem {

    public enum EstadoItem { PENDIENTE, PAGADO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "liquidacion_id", nullable = false)
    private LiquidacionSueldo liquidacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;

    @Column(name = "sueldo_base", nullable = false, precision = 15, scale = 2)
    private BigDecimal sueldoBase = BigDecimal.ZERO;

    /**
     * Días no trabajados a descontar del sueldo fijo en esta liquidación
     * (faltas, francos, etc.). Default 0.
     */
    @Column(name = "dias_descontados", nullable = false)
    private Integer diasDescontados = 0;

    @Column(name = "ventas_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal ventasTotal = BigDecimal.ZERO;

    @Column(name = "cantidad_ventas", nullable = false)
    private Integer cantidadVentas = 0;

    @Column(name = "cantidad_articulos", nullable = false)
    private Integer cantidadArticulos = 0;

    @Column(name = "porcentaje_comision", nullable = false, precision = 9, scale = 4)
    private BigDecimal porcentajeComision = BigDecimal.ZERO;

    @Column(name = "comision_monto", nullable = false, precision = 15, scale = 2)
    private BigDecimal comisionMonto = BigDecimal.ZERO;

    @Column(name = "total", nullable = false, precision = 15, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoItem estado = EstadoItem.PENDIENTE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_pago_id")
    private CuentaFinanciera cuentaPago;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movimiento_id")
    private MovimientoFinanciero movimiento;

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
