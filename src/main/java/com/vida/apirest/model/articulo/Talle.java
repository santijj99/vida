package com.vida.apirest.model.articulo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
        name = "talle",
        uniqueConstraints = @UniqueConstraint(name = "uk_talle_pais_numero", columnNames = {"pais", "numero"}),
        indexes = @Index(name = "ix_talle_pais", columnList = "pais")
)
public class Talle {

    public enum Pais { AR, UK, BR, US, EU }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    private Pais pais;

    @Column(length = 30, nullable = false)
    private String numero;

    @Column(length = 255)
    private String descripcion;

}