package com.vida.apirest.model.terceros;

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
        name = "movimiento_cuenta_corriente",
        indexes = {
                @Index(name = "ix_mov_cc_cuenta", columnList = "cuenta_corriente_id"),
                @Index(name = "ix_mov_cc_numero", columnList = "numero", unique = true)
        }
)
public class MovimientoCuentaCorriente {

    public enum TipoMovimiento { DEBE, HABER, SALDO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_corriente_id", nullable = false)
    private TerceroCuentaCorriente cuentaCorriente;

    @Column(name = "numero", nullable = false, length = 50, unique = true)
    private String numero;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoMovimiento tipo;

    @Column(name = "monto", nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Column(name = "saldo_deudor", precision = 15, scale = 2)
    private BigDecimal saldoDeudor = BigDecimal.ZERO;

    @Column(name = "saldo_acreedor", precision = 15, scale = 2)
    private BigDecimal saldoAcreedor = BigDecimal.ZERO;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "referencia", length = 255)
    private String referencia; // Comprobante, factura, etc.

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
