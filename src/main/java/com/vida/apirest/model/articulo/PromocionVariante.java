package com.vida.apirest.model.articulo;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
@Table(
        name = "promocion_variante",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_promocion_variante",
                columnNames = {"promocion_id", "variante_id"}
        ),
        indexes = {
                @Index(name = "ix_promocion_variante_promo", columnList = "promocion_id"),
                @Index(name = "ix_promocion_variante_var", columnList = "variante_id")
        }
)
public class PromocionVariante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promocion_id", nullable = false)
    private Promocion promocion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variante_id", nullable = false)
    private VarianteArticulo variante;

    @Column(name = "precio_promocional", precision = 12, scale = 2)
    private BigDecimal precioPromocional;
}
