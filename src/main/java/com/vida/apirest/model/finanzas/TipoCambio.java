package com.vida.apirest.model.finanzas;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "tipo_cambio",
        indexes = {
                @Index(name = "ix_tipo_cambio_moneda_fecha", columnList = "moneda_id, fecha", unique = true),
                @Index(name = "ix_tipo_cambio_fecha", columnList = "fecha")
        }
)
public class TipoCambio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "moneda_id", nullable = false)
    private Moneda moneda;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "tasa_compra", precision = 15, scale = 6, nullable = false)
    private BigDecimal tasaCompra;

    @Column(name = "tasa_venta", precision = 15, scale = 6, nullable = false)
    private BigDecimal tasaVenta;

    @Column(name = "tasa_promedio", precision = 15, scale = 6)
    private BigDecimal tasaPromedio;

    @Column(name = "fuente", length = 100)
    private String fuente; // Ej: "BCRA", "DOLAR_BLUE", "MANUAL"

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "usuario", length = 100)
    private String usuario;
}