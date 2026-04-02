package com.vida.apirest.model.articulo;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;


@Entity
@Table(
        name = "lista_precio",
        uniqueConstraints = @UniqueConstraint(name = "uk_lista_precio", columnNames = "precio"),
        indexes = @Index(name = "ix_lista_precio", columnList = "precio", unique = true)
)
public class ListaPrecio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal precio;

    @Column(name = "costo", precision = 12, scale = 2)
    private BigDecimal costo;

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public BigDecimal getCosto() {
        return costo;
    }

    public void setCosto(BigDecimal costo) {
        this.costo = costo;
    }
}
