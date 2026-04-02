package com.vida.apirest.model.articulo;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "taxon_articulo",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_taxon_articulo",
        columnNames = {"articulo_id", "taxon_id"}
    ),
    indexes = {
        @Index(name = "ix_ta_articulo", columnList = "articulo_id"),
        @Index(name = "ix_ta_taxon", columnList = "taxon_id")
    }
)
public class TaxonArticulo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "articulo_id", nullable = false)
    private Long articuloId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "articulo_id", insertable = false, updatable = false)
    private Articulo articulo;

    @Column(name = "taxon_id", nullable = false)
    private Long taxonId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "taxon_id", insertable = false, updatable = false)
    private Taxon taxon;

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

    public Long getTaxonId() {
        return taxonId;
    }

    public void setTaxonId(Long taxonId) {
        this.taxonId = taxonId;
    }

    public Taxon getTaxon() {
        return taxon;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
