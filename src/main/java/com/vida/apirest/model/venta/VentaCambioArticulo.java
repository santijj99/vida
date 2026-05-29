package com.vida.apirest.model.venta;

import com.vida.apirest.model.articulo.VarianteArticulo;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "venta_cambio_articulo")
public class VentaCambioArticulo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venta_id", nullable = false)
    private Venta venta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_detalle_id")
    private VentaDetalle ventaDetalle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variante_devuelta_id")
    private VarianteArticulo varianteDevuelta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variante_nueva_id", nullable = false)
    private VarianteArticulo varianteNueva;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad = 1;

    @Column(name = "motivo", nullable = false, columnDefinition = "TEXT")
    private String motivo;

    @Column(name = "precio_anterior", precision = 15, scale = 2)
    private BigDecimal precioAnterior;

    @Column(name = "precio_nuevo", precision = 15, scale = 2)
    private BigDecimal precioNuevo;

    @Column(name = "diferencia_precio", precision = 15, scale = 2)
    private BigDecimal diferenciaPrecio;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
