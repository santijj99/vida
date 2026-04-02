package com.vida.apirest.model.articulo;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(
        name = "marca",
        uniqueConstraints = @UniqueConstraint(name = "uk_marca_nombre", columnNames = "nombre"),
        indexes = @Index(name = "ix_marca_nombre", columnList = "nombre", unique = true)
)
public class Marca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 120, nullable = false)
    private String nombre;

    // getters/setters
    public Long getId() { return id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}