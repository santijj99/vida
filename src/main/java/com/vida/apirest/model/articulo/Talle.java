package com.vida.apirest.model.articulo;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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