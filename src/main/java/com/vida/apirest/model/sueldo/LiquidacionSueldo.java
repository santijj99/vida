package com.vida.apirest.model.sueldo;

import com.vida.apirest.model.almacen.Sucursal;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "liquidacion_sueldo",
        indexes = {
                @Index(name = "ix_liq_sueldo_numero", columnList = "numero", unique = true),
                @Index(name = "ix_liq_sueldo_estado", columnList = "estado"),
                @Index(name = "ix_liq_sueldo_fechas", columnList = "fecha_desde,fecha_hasta")
        }
)
public class LiquidacionSueldo {

    public enum EstadoLiquidacion {
        /** Reservado / legado; las liquidaciones nacen en CALCULADA. */
        BORRADOR,
        CALCULADA,
        PAGADA_PARCIAL,
        PAGADA,
        CANCELADA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "numero", nullable = false, length = 50, unique = true)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id")
    private Sucursal sucursal;

    @Enumerated(EnumType.STRING)
    @Column(name = "periodo_tipo", nullable = false, length = 20)
    private PeriodoSueldo periodoTipo = PeriodoSueldo.MES;

    @Column(name = "fecha_desde", nullable = false)
    private LocalDate fechaDesde;

    @Column(name = "fecha_hasta", nullable = false)
    private LocalDate fechaHasta;

    /** Si se informa, pisa el % de comisión de cada empleado en esta liquidación. */
    @Column(name = "porcentaje_comision_override", precision = 9, scale = 4)
    private BigDecimal porcentajeComisionOverride;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoLiquidacion estado = EstadoLiquidacion.CALCULADA;

    @Column(name = "total_sueldos", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalSueldos = BigDecimal.ZERO;

    @Column(name = "total_comisiones", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalComisiones = BigDecimal.ZERO;

    @Column(name = "total_general", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalGeneral = BigDecimal.ZERO;

    @Column(name = "responsable", length = 255)
    private String responsable;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "liquidacion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<LiquidacionSueldoItem> items = new ArrayList<>();
}
