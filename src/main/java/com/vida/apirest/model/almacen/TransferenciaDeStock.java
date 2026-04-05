package com.vida.apirest.model.almacen;

import com.vida.apirest.model.articulo.Articulo;
import com.vida.apirest.model.articulo.VarianteArticulo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "transferencia_stock",
        indexes = {
                @Index(name = "ix_trans_origen", columnList = "deposito_origen_id"),
                @Index(name = "ix_trans_destino", columnList = "deposito_destino_id"),
                @Index(name = "ix_trans_numero", columnList = "numero", unique = true)
        }
)
public class TransferenciaDeStock {

    public enum EstadoTransferencia {
        PENDIENTE,
        EN_TRANSITO,
        RECIBIDA_PARCIAL,
        RECIBIDA_TOTAL,
        CANCELADA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero", nullable = false, length = 50, unique = true)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deposito_origen_id", nullable = false)
    private Deposito depositoOrigen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deposito_destino_id", nullable = false)
    private Deposito depositoDestino;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoTransferencia estado = EstadoTransferencia.PENDIENTE;

    @Column(name = "responsable_origen", length = 255)
    private String responsableOrigen;

    @Column(name = "responsable_destino", length = 255)
    private String responsableDestino;

    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;

    @Column(name = "fecha_recepcion")
    private LocalDateTime fechaRecepcion;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Relaciones
    @OneToMany(mappedBy = "transferencia", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<TransferenciaDetalleStock> detalles = new HashSet<>();
}
