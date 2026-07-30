package com.vida.apirest.model.credito;

import com.vida.apirest.model.tesoreria.MovimientoFinanciero;
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
        name = "pago_cuota",
        indexes = {
                @Index(name = "ix_pago_cuota_cuota", columnList = "cuota_id"),
                @Index(name = "ix_pago_cuota_estado", columnList = "estado")
        }
)
public class PagoCuota {

    public enum EstadoPagoCuota { ACTIVO, ANULADO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuota_id", nullable = false)
    private Cuota cuota;

    @Column(name = "monto", nullable = false, precision = 15, scale = 2)
    private BigDecimal monto = BigDecimal.ZERO;

    /** Parte del monto aplicada al recargo por mora; el resto va a capital. */
    @Column(name = "monto_recargo", precision = 15, scale = 2)
    private BigDecimal montoRecargo = BigDecimal.ZERO;

    @Column(name = "metodo_pago", nullable = false, length = 50)
    private String metodoPago;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoPagoCuota estado = EstadoPagoCuota.ACTIVO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movimiento_financiero_id")
    private MovimientoFinanciero movimientoFinanciero;

    @Column(name = "motivo_anulacion", columnDefinition = "TEXT")
    private String motivoAnulacion;

    @Column(name = "fecha_anulacion")
    private LocalDateTime fechaAnulacion;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
