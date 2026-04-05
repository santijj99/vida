package com.vida.apirest.model.articulo;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(
    name = "historial_precio",
    indexes = {
        @Index(name = "ix_hist_precio_variante", columnList = "variante_articulo_id"),
        @Index(name = "ix_hist_precio_fecha", columnList = "fecha")
    }
)
public class HistorialPrecio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "variante_articulo_id", nullable = false)
    private Long varianteArticuloId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variante_articulo_id", insertable = false, updatable = false)
    private VarianteArticulo varianteArticulo;

    @Column(name = "precio_anterior", precision = 12, scale = 2)
    private BigDecimal precioAnterior;

    @Column(name = "precio_nuevo", precision = 12, scale = 2, nullable = false)
    private BigDecimal precioNuevo;

    @Column(name = "costo_anterior", precision = 12, scale = 2)
    private BigDecimal costoAnterior;

    @Column(name = "costo_nuevo", precision = 12, scale = 2)
    private BigDecimal costoNuevo;

    @Column(name = "fecha", nullable = false, updatable = false)
    private LocalDateTime fecha;

    @Column(name = "motivo", length = 255)
    private String motivo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
