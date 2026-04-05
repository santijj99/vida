package com.vida.apirest.model.tesoreria;

import com.vida.apirest.model.finanzas.CuentaFinanciera;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "transferencia_financiera",
        indexes = {
                @Index(name = "ix_trans_fin_origen", columnList = "cuenta_origen_id"),
                @Index(name = "ix_trans_fin_destino", columnList = "cuenta_destino_id"),
                @Index(name = "ix_trans_fin_numero", columnList = "numero", unique = true)
        }
)
public class TransferenciaFinanciera {

    public enum EstadoTransferencia {
        PENDIENTE,
        PROCESADA,
        RECHAZADA,
        CANCELADA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero", nullable = false, length = 50, unique = true)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_origen_id", nullable = false)
    private CuentaFinanciera cuentaOrigen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_destino_id", nullable = false)
    private CuentaFinanciera cuentaDestino;

    @Column(name = "monto", nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoTransferencia estado = EstadoTransferencia.PENDIENTE;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "motivo", length = 255)
    private String motivo;

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
}
