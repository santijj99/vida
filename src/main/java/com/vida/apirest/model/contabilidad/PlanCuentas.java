package com.vida.apirest.model.contabilidad;

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
        name = "plan_cuentas",
        indexes = {
                @Index(name = "ix_plan_cuentas_empresa", columnList = "empresa_id"),
                @Index(name = "ix_plan_cuentas_codigo", columnList = "codigo", unique = true)
        }
)
public class PlanCuentas {

    public enum TipoCuenta { ACTIVO, PASIVO, PATRIMONIO, INGRESO, GASTO, COSTO }

    public enum Naturaleza { DEUDORA, ACREEDORA }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @Column(name = "codigo", nullable = false, length = 50, unique = true)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 255)
    private String nombre;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoCuenta tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "naturaleza", nullable = false)
    private Naturaleza naturaleza;

    @Column(name = "nivel", nullable = false)
    private Integer nivel = 1;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "cuenta_padre_id")
    private Long cuentaPadreId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_padre_id", insertable = false, updatable = false)
    private PlanCuentas cuentaPadre;

    @OneToMany(mappedBy = "cuentaPadre", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<PlanCuentas> cuentasHijas = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

