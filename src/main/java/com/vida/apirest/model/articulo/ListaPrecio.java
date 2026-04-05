package com.vida.apirest.model.articulo;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;



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
