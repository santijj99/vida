package com.vida.apirest.model.articulo;
import jakarta.persistence.*;
import java.time.LocalDateTime;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Data
@Entity
@Table(
    name = "taxon",
    indexes = {
        @Index(name = "ix_taxon_nombre", columnList = "nombre"),
        @Index(name = "ix_taxon_padre", columnList = "taxon_padre_id")
    }
)
public class Taxon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "nombre", length = 120, nullable = false)
    private String nombre;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "taxon_padre_id")
    private Long taxonPadreId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taxon_padre_id", insertable = false, updatable = false)
    private Taxon taxonPadre;

    @Column(name = "nivel", nullable = false)
    private Integer nivel = 0;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;


}
