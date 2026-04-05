package com.vida.apirest.model.contabilidad;

import com.vida.apirest.model.almacen.Sucursal;
import com.vida.apirest.model.empresa.Empresa;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "asiento_contable",
        indexes = {
                @Index(name = "ix_asiento_empresa", columnList = "empresa_id"),
                @Index(name = "ix_asiento_sucursal", columnList = "sucursal_id"),
                @Index(name = "ix_asiento_ejercicio", columnList = "ejercicio_id"),
                @Index(name = "ix_asiento_periodo", columnList = "periodo_id"),
                @Index(name = "ix_asiento_numero", columnList = "numero", unique = true)
        }
)
public class AsientoContable {

    public enum TipoAsiento { DIARIO, MAYOR, AJUSTE, APERTURA, CIERRE }

    public enum EstadoAsiento { BORRADOR, CONFIRMADO, ANULADO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id")
    private Sucursal sucursal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ejercicio_id")
    private EjercicioContable ejercicio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "periodo_id")
    private PeriodoContable periodo;

    @Column(name = "numero", nullable = false, length = 50, unique = true)
    private String numero;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoAsiento tipo = TipoAsiento.DIARIO;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoAsiento estado = EstadoAsiento.BORRADOR;

    @Column(name = "referencia", length = 255)
    private String referencia;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "total_debe", precision = 15, scale = 2)
    private java.math.BigDecimal totalDebe;

    @Column(name = "total_haber", precision = 15, scale = 2)
    private java.math.BigDecimal totalHaber;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "asiento", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<AsientoContableDetalle> detalles = new HashSet<>();
}

