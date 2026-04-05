package com.vida.apirest.model.terceros;

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
        name = "tercero",
        indexes = {
                @Index(name = "ix_tercero_tipo", columnList = "tipo"),
                @Index(name = "ix_tercero_codigo", columnList = "codigo", unique = true)
        }
)
public class Tercero {

    public enum TipoTercero { PROVEEDOR, CLIENTE_MAYORISTA, TRANSPORTISTA, OTROS }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false, length = 50, unique = true)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 255)
    private String nombre;

    @Column(name = "nombre_corto", length = 100)
    private String nombreCorto;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoTercero tipo;

    @Column(name = "cuit_cuil", length = 20)
    private String cuitCuil;

    @Column(name = "domicilio", length = 500)
    private String domicilio;

    @Column(name = "ciudad", length = 100)
    private String ciudad;

    @Column(name = "provincia", length = 100)
    private String provincia;

    @Column(name = "codigo_postal", length = 20)
    private String codigoPostal;

    @Column(name = "pais", length = 100)
    private String pais;

    @Column(name = "telefono", length = 50)
    private String telefono;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "contacto", length = 255)
    private String contacto;

    @Column(name = "condicion_pago", length = 100)
    private String condicionPago;

    @Column(name = "limites_credito")
    private String limiteCredito;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Relaciones
    @OneToMany(mappedBy = "tercero", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<TerceroCuentaCorriente> cuentasCorrientes = new HashSet<>();
}
