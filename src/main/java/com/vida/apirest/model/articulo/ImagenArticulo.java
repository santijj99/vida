package com.vida.apirest.model.articulo;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getArticuloId() {
        return articuloId;
    }

    public void setArticuloId(Long articuloId) {
        this.articuloId = articuloId;
    }

    public Articulo getArticulo() {
        return articulo;
    }

    public Long getImagenId() {
        return imagenId;
    }

    public void setImagenId(Long imagenId) {
        this.imagenId = imagenId;
    }

    public Imagen getImagen() {
        return imagen;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }

    public Boolean getPrincipal() {
        return principal;
    }

    public void setPrincipal(Boolean principal) {
        this.principal = principal;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
