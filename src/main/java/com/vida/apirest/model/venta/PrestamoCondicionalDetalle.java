package com.vida.apirest.model.venta;

import com.vida.apirest.model.articulo.Articulo;
import com.vida.apirest.model.articulo.VarianteArticulo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "prestamo_condicional_detalle")
public class PrestamoCondicionalDetalle {

    public enum EstadoDetalle {
        PENDIENTE,
        DEVUELTO,
        CONFIRMADO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prestamo_id", nullable = false)
    private PrestamoCondicional prestamo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "articulo_id", nullable = false)
    private Articulo articulo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variante_id")
    private VarianteArticulo variante;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario_referencia", precision = 15, scale = 2)
    private BigDecimal precioUnitarioReferencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoDetalle estado = EstadoDetalle.PENDIENTE;
}
