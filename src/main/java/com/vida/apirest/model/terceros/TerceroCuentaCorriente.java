package com.vida.apirest.model.terceros;

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
        name = "tercero_cuenta_corriente",
        indexes = @Index(name = "ix_tercero_cc_tercero", columnList = "tercero_id")
)
public class TerceroCuentaCorriente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tercero_id", nullable = false)
    private Tercero tercero;

    @Column(name = "saldo_deudor", precision = 15, scale = 2)
    private BigDecimal saldoDeudor = BigDecimal.ZERO;

    @Column(name = "saldo_acreedor", precision = 15, scale = 2)
    private BigDecimal saldoAcreedor = BigDecimal.ZERO;

    @Column(name = "limite_credito", precision = 15, scale = 2)
    private BigDecimal limiteCredito;

    @Column(name = "credito_disponible", precision = 15, scale = 2)
    private BigDecimal creditoDisponible;

    @Column(name = "dias_vencimiento")
    private Integer diasVencimiento;

    @Column(name = "ultima_operacion")
    private LocalDateTime ultimaOperacion;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Relaciones
    @OneToMany(mappedBy = "cuentaCorriente", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<MovimientoCuentaCorriente> movimientos = new HashSet<>();
}
