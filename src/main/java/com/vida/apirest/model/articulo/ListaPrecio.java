package com.vida.apirest.model.articulo;

import java.math.BigDecimal;

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
}
