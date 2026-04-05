package com.vida.apirest.model.articulo;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(
        name = "variante_articulo",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_variante_articulo_color_talle",
                        columnNames = {"articulo_id", "color_id", "talle_id"}
                ),
                @UniqueConstraint(
                        name = "uk_variante_codigo_barras",
                        columnNames = {"codigo_barras"}
                )
        },
        indexes = {
                @Index(name = "ix_var_articulo", columnList = "articulo_id"),
                @Index(name = "ix_var_color", columnList = "color_id"),
                @Index(name = "ix_var_talle", columnList = "talle_id"),
                @Index(name = "ix_var_lista_precio", columnList = "lista_precio_id")
        }
)
public class VarianteArticulo {

    public enum EstadoVariante { ACTIVO, INACTIVO, PENDIENTE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "articulo_id", nullable = false)
    private Long articuloId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "articulo_id", insertable = false, updatable = false)
    private Articulo articulo;

    @Column(name = "color_id")
    private Long colorId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "color_id", insertable = false, updatable = false)
    private Color color;


    @Column(name = "lista_precio_id")
    private Long listaPrecioId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "lista_precio_id", insertable = false, updatable = false)
    private ListaPrecio listaPrecio;



    @Column(name = "talle_id")
    private Long talleId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "talle_id", insertable = false, updatable = false)
    private Talle talle;

    @Column(name = "codigo_barras", length = 64, unique = true)
    private String codigoBarras;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoVariante estado = EstadoVariante.ACTIVO;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Relación bidireccional con HistorialPrecio
    @OneToMany(mappedBy = "varianteArticulo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HistorialPrecio> historialPrecios;

}