package com.vida.apirest.model.articulo;


import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(
        name = "color",
        uniqueConstraints = @UniqueConstraint(name = "uk_color_nombre", columnNames = "nombre"),
        indexes = @Index(name = "ix_color_nombre", columnList = "nombre", unique = true)
)
public class Color {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 60, nullable = false)
    private String nombre;

    // getters/setters
    public Long getId() { return id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}