package com.vida.apirest.model.finanzas;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "caja_sesion",
        indexes = {
                @Index(name = "ix_caja_sesion_cuenta", columnList = "cuenta_id"),
                @Index(name = "ix_caja_sesion_estado", columnList = "estado")
        }
)
public class CajaSesion {

    public enum EstadoSesion { ABIERTA, CERRADA }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private CuentaFinanciera cuenta;

    @Column(name = "fecha_apertura", nullable = false)
    private LocalDateTime fechaApertura;

    /** Efectivo con el que se abre el turno (fondo inicial en el cajón). */
    @Column(name = "monto_apertura", nullable = false, precision = 15, scale = 2)
    private BigDecimal montoApertura = BigDecimal.ZERO;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @Column(name = "total_ingresos", precision = 15, scale = 2)
    private BigDecimal totalIngresos = BigDecimal.ZERO;

    @Column(name = "total_egresos", precision = 15, scale = 2)
    private BigDecimal totalEgresos = BigDecimal.ZERO;

    @Column(name = "monto_esperado_cierre", precision = 15, scale = 2)
    private BigDecimal montoEsperadoCierre;

    @Column(name = "monto_contado_cierre", precision = 15, scale = 2)
    private BigDecimal montoContadoCierre;

    @Column(name = "diferencia", precision = 15, scale = 2)
    private BigDecimal diferencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoSesion estado = EstadoSesion.ABIERTA;

    @Column(name = "abierto_por", length = 255)
    private String abiertoPor;

    @Column(name = "cerrado_por", length = 255)
    private String cerradoPor;

    @Column(name = "observaciones_cierre", columnDefinition = "TEXT")
    private String observacionesCierre;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
