package com.vida.apirest.model.credito;

import com.vida.apirest.model.persona.Cliente;
import com.vida.apirest.model.almacen.Sucursal;
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
        name = "credito",
        indexes = {
                @Index(name = "ix_credito_cliente", columnList = "cliente_id"),
                @Index(name = "ix_credito_numero", columnList = "numero", unique = true),
                @Index(name = "ix_credito_sucursal", columnList = "sucursal_id")
        }
)
public class Credito {

    public enum EstadoCredito { ACTIVO, VENCIDO, PAGADO, CANCELADO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id")
    private Sucursal sucursal;

    @Column(name = "numero", nullable = false, length = 50, unique = true)
    private String numero;

    @Column(name = "importe", nullable = false, precision = 15, scale = 2)
    private BigDecimal importe = BigDecimal.ZERO;

    @Column(name = "saldo", precision = 15, scale = 2)
    private BigDecimal saldo = BigDecimal.ZERO;

    @Column(name = "plazo_meses")
    private Integer plazoMeses;

    @Column(name = "tasa_interes", precision = 9, scale = 4)
    private BigDecimal tasaInteres = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoCredito estado = EstadoCredito.ACTIVO;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "credito", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<Cuota> cuotas = new HashSet<>();
}

