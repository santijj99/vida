package com.vida.apirest.model.articulo;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Data
@Entity
@Table(
        name = "talle",
        uniqueConstraints = @UniqueConstraint(name = "uk_talle_etiqueta", columnNames = "etiqueta"),
        indexes = @Index(name = "ix_talle_etiqueta", columnList = "etiqueta", unique = true)
)
public class Talle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 30, nullable = false)
    private String etiqueta;

    @Column(length = 30, nullable = false)
    private String numero;

}