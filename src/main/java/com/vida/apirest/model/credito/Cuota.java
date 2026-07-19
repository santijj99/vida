package com.vida.apirest.model.credito;

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
        name = "cuota",
        indexes = {
                @Index(name = "ix_cuota_credito", columnList = "credito_id"),
                @Index(name = "ix_cuota_numero", columnList = "numero"),
                @Index(name = "ix_cuota_estado", columnList = "estado")
        }
)
public class Cuota {

    public enum EstadoCuota { PENDIENTE, PAGADA, VENCIDA, CANCELADA, ELIMINADA }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credito_id")
    private Credito credito;

    @Column(name = "numero", nullable = false, length = 50)
    private String numero;

    @Column(name = "fecha_vencimiento")
    private LocalDateTime fechaVencimiento;

    @Column(name = "monto", nullable = false, precision = 15, scale = 2)
    private BigDecimal monto = BigDecimal.ZERO;

    @Column(name = "saldo", precision = 15, scale = 2)
    private BigDecimal saldo = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoCuota estado = EstadoCuota.PENDIENTE;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "recargo", precision = 15, scale = 2)
    private BigDecimal recargo = BigDecimal.ZERO;

    @Column(name = "recargo_exento")
    private Boolean recargoExento = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

