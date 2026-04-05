package com.vida.apirest.model.finanzas;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "moneda",
        indexes = @Index(name = "ix_moneda_codigo", columnList = "codigo", unique = true)
)
public class Moneda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false, length = 3, unique = true)
    private String codigo; // ARS, USD, EUR, etc.

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "simbolo", length = 5)
    private String simbolo;

    @Column(name = "tasa_cambio", precision = 15, scale = 6)
    private BigDecimal tasaCambio = BigDecimal.ONE;

    @Column(name = "decimal_places", nullable = false)
    private Integer decimalPlaces = 2;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "predeterminada", nullable = false)
    private Boolean predeterminada = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Relaciones
    @OneToMany(mappedBy = "moneda", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<CuentaFinanciera> cuentasFinancieras = new HashSet<>();
}
