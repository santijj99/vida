package com.vida.apirest.model.sueldo;

import com.vida.apirest.model.persona.Empleado;
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
        name = "empleado_sueldo_config",
        indexes = {
                @Index(name = "ix_empleado_sueldo_config_empleado", columnList = "empleado_id", unique = true)
        }
)
public class EmpleadoSueldoConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empleado_id", nullable = false, unique = true)
    private Empleado empleado;

    /** Monto fijo correspondiente al periodoBase (ej. sueldo mensual o diario). */
    @Column(name = "sueldo_fijo", nullable = false, precision = 15, scale = 2)
    private BigDecimal sueldoFijo = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "periodo_base", nullable = false, length = 20)
    private PeriodoSueldo periodoBase = PeriodoSueldo.MES;

    @Column(name = "porcentaje_comision", nullable = false, precision = 9, scale = 4)
    private BigDecimal porcentajeComision = BigDecimal.ZERO;

    /**
     * Días de la semana que trabaja (ISO 1=lun … 7=dom), CSV ej. {@code 1,2,3,4,5}.
     * Solo aplica al prorrateo con {@code periodoBase = DIA}.
     */
    @Column(name = "dias_laborables", length = 32)
    private String diasLaborables;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
