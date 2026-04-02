package com.vida.apirest.model.articulo;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
    private String nombre;

    // getters/setters
    public Long getId() { return id; }

    public String getEtiqueta() { return etiqueta; }
    public void setEtiqueta(String etiqueta) { this.etiqueta = etiqueta; }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}