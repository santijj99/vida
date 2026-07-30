package com.vida.apirest.model.finanzas;

import com.vida.apirest.model.almacen.Sucursal;
import com.vida.apirest.model.persona.Empleado;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "FinanzasCuentaFinanciera")
@Table(
        name = "cuenta_financiera",
        indexes = {
                @Index(name = "ix_cuenta_sucursal", columnList = "sucursal_id"),
                @Index(name = "ix_cuenta_numero", columnList = "numero", unique = true),
                @Index(name = "ix_cuenta_empleado", columnList = "empleado_id")
        }
)
public class CuentaFinanciera {

    public enum TipoCuenta { CAJA, BANCO, AHORRO, TARJETA_DEBITO, TARJETA_CREDITO, BILLETERA, VIRTUAL }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "moneda_id", nullable = false)
    private Moneda moneda;

    @Column(name = "nombre", nullable = false, length = 255)
    private String nombre;

    @Column(name = "numero", nullable = false, length = 100, unique = true)
    private String numero;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoCuenta tipo;

    @Column(name = "banco", length = 255)
    private String banco;

    @Column(name = "saldo_inicial", precision = 15, scale = 2)
    private BigDecimal saldoInicial = BigDecimal.ZERO;

    @Column(name = "saldo_actual", precision = 15, scale = 2, nullable = false)
    private BigDecimal saldoActual = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id")
    private Empleado empleadoResponsable;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
