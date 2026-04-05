package com.vida.apirest.model.almacen;

import com.vida.apirest.model.articulo.Articulo;
import com.vida.apirest.model.articulo.VarianteArticulo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "transferencia_detalle_stock",
        indexes = {
                @Index(name = "ix_transf_det_transferencia", columnList = "transferencia_id"),
                @Index(name = "ix_transf_det_articulo", columnList = "articulo_id")
        }
)
public class TransferenciaDetalleStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transferencia_id", nullable = false)
    private TransferenciaDeStock transferencia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "articulo_id", nullable = false)
    private Articulo articulo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variante_id")
    private VarianteArticulo variante;

    @Column(name = "cantidad_enviada", nullable = false)
    private Integer cantidadEnviada = 0;

    @Column(name = "cantidad_recibida", nullable = false)
    private Integer cantidadRecibida = 0;

    @Column(name = "lote", length = 100)
    private String lote;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;
}
