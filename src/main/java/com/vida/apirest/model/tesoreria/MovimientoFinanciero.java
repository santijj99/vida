package com.vida.apirest.model.tesoreria;

import com.vida.apirest.model.finanzas.CuentaFinanciera;
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
        name = "movimiento_financiero",
        indexes = {
                @Index(name = "ix_mov_financiero_cuenta", columnList = "cuenta_id"),
                @Index(name = "ix_mov_financiero_numero", columnList = "numero", unique = true),
                @Index(name = "ix_mov_financiero_tipo", columnList = "tipo")
        }
)
public class MovimientoFinanciero {

    public enum TipoMovimiento { INGRESO, EGRESO, TRANSFERENCIA_ENVIADA, TRANSFERENCIA_RECIBIDA, AJUSTE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private CuentaFinanciera cuenta;

    @Column(name = "numero", nullable = false, length = 50, unique = true)
    private String numero;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoMovimiento tipo;

    @Column(name = "monto", nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Column(name = "saldo_anterior", precision = 15, scale = 2)
    private BigDecimal saldoAnterior;

    @Column(name = "saldo_nuevo", precision = 15, scale = 2)
    private BigDecimal saldoNuevo;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "referencia", length = 255)
    private String referencia; // Número de comprobante, factura, etc.

    @Column(name = "responsable", length = 255)
    private String responsable;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
