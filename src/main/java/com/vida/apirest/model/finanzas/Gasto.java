package com.vida.apirest.model.finanzas;

import com.vida.apirest.model.almacen.Sucursal;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "gasto",
        indexes = {
                @Index(name = "ix_gasto_sucursal", columnList = "sucursal_id"),
                @Index(name = "ix_gasto_numero", columnList = "numero", unique = true),
                @Index(name = "ix_gasto_categoria", columnList = "categoria_id")
        }
)
public class Gasto {

    public enum EstadoGasto { BORRADOR, APROBADO, PAGADO, CANCELADO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private GastoCategoria categoria;

    @Column(name = "numero", nullable = false, length = 50, unique = true)
    private String numero;

    @Column(name = "descripcion", nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "monto", nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Column(name = "moneda_id")
    private Long monedaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoGasto estado = EstadoGasto.BORRADOR;

    @Column(name = "proveedor", length = 255)
    private String proveedor;

    @Column(name = "numero_comprobante", length = 50)
    private String numeroComprobante;

    @Column(name = "fecha_comprobante")
    private LocalDateTime fechaComprobante;

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

    // Relaciones
    @OneToMany(mappedBy = "gasto", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<GastoPago> pagos = new HashSet<>();
}
