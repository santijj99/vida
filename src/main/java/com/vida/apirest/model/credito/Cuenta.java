package com.vida.apirest.model.credito;

import com.vida.apirest.model.persona.Cliente;
import com.vida.apirest.model.almacen.Sucursal;
import com.vida.apirest.model.finanzas.Moneda;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "credito_cuenta",
        indexes = {
                @Index(name = "ix_credito_cuenta_cliente", columnList = "cliente_id"),
                @Index(name = "ix_credito_cuenta_sucursal", columnList = "sucursal_id"),
                @Index(name = "ix_credito_cuenta_numero", columnList = "numero", unique = true)
        }
)
public class Cuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id")
    private Sucursal sucursal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moneda_id")
    private Moneda moneda;

    @Column(name = "numero", nullable = false, length = 50, unique = true)
    private String numero;

    @Column(name = "limite_credito", precision = 15, scale = 2)
    private BigDecimal limiteCredito = BigDecimal.ZERO;

    @Column(name = "saldo_actual", precision = 15, scale = 2)
    private BigDecimal saldoActual = BigDecimal.ZERO;

    @Column(name = "tasa_interes", precision = 9, scale = 4)
    private BigDecimal tasaInteres = BigDecimal.ZERO;

    @Column(name = "dia_vencimiento")
    private Integer diaVencimiento;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

