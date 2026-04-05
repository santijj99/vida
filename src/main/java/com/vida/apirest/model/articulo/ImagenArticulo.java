package com.vida.apirest.model.articulo;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "imagen_articulo",
    indexes = {
        @Index(name = "ix_ia_articulo", columnList = "articulo_id"),
        @Index(name = "ix_ia_imagen", columnList = "imagen_id"),
        @Index(name = "ix_ia_orden", columnList = "orden")
    }
)
@Data
public class ImagenArticulo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "articulo_id", nullable = false)
    private Long articuloId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "articulo_id", insertable = false, updatable = false)
    private Articulo articulo;

    @Column(name = "imagen_id", nullable = false)
    private Long imagenId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "imagen_id", insertable = false, updatable = false)
    private Imagen imagen;

    @Column(name = "orden", nullable = false)
    private Integer orden = 0;

    @Column(name = "principal", nullable = false)
    private Boolean principal = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
