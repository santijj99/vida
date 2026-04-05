package com.vida.apirest.model.contabilidad;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "asiento_contable_detalle",
        indexes = {
                @Index(name = "ix_as_det_asiento", columnList = "asiento_id"),
                @Index(name = "ix_as_det_cuenta", columnList = "cuenta_id"),
                @Index(name = "ix_as_det_cc", columnList = "centro_costos_id")
        }
)
public class AsientoContableDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asiento_id", nullable = false)
    private AsientoContable asiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_id")
    private PlanCuentas cuenta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "centro_costos_id")
    private CentroCostos centroCostos;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "debe", precision = 15, scale = 2)
    private BigDecimal debe = BigDecimal.ZERO;

    @Column(name = "haber", precision = 15, scale = 2)
    private BigDecimal haber = BigDecimal.ZERO;

    @Column(name = "referencia", length = 255)
    private String referencia;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

