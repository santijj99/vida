package com.vida.apirest.model.articulo;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Data
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "taxon_id", insertable = false, updatable = false)
    private Taxon taxon;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
