package com.vida.apirest.model.almacen;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "stock_movimiento",
        indexes = {
                @Index(name = "ix_stock_mov_stock", columnList = "stock_id"),
                @Index(name = "ix_stock_mov_tipo", columnList = "tipo"),
                @Index(name = "ix_stock_mov_fecha", columnList = "created_at")
        }
)
public class StockMovimiento {

    public enum TipoMovimiento {
        INGRESO_COMPRA,
        INGRESO_DEVOLUCION,
        INGRESO_AJUSTE,
        SALIDA_VENTA,
        SALIDA_DEVOLUCION,
        SALIDA_AJUSTE,
        SALIDA_TRANSFERENCIA,
        INGRESO_TRANSFERENCIA,
        SALIDA_MERMA,
        SALIDA_OBSEQUIO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoMovimiento tipo;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "saldo_anterior", nullable = false)
    private Integer saldoAnterior;

    @Column(name = "saldo_nuevo", nullable = false)
    private Integer saldoNuevo;

    @Column(name = "referencia", length = 255)
    private String referencia; // Número de comprobante, OC, etc.

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "usuario", length = 100)
    private String usuario;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
