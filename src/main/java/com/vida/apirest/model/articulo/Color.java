package com.vida.apirest.model.articulo;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
 

@Data
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
}