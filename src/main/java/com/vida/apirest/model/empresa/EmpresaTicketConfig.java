package com.vida.apirest.model.empresa;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "empresa_ticket_config",
        uniqueConstraints = @UniqueConstraint(name = "uk_empresa_ticket_config", columnNames = "empresa_id")
)
public class EmpresaTicketConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false, unique = true)
    private Empresa empresa;

    @Enumerated(EnumType.STRING)
    @Column(name = "formato", nullable = false, length = 20)
    private FormatoTicketPdf formato = FormatoTicketPdf.TERMICO_80MM;

    @Column(name = "abrir_automaticamente", nullable = false)
    private Boolean abrirAutomaticamente = true;

    /** Cabecera de comprobantes no fiscales (venta, crédito, cobro de cuotas). */
    @Column(name = "cabecera_razon_social", length = 255)
    private String cabeceraRazonSocial;

    @Column(name = "cabecera_direccion", length = 500)
    private String cabeceraDireccion;

    @Column(name = "cabecera_cuit", length = 20)
    private String cabeceraCuit;

    @Column(name = "cabecera_condicion_iva", length = 120)
    private String cabeceraCondicionIva;

    @Column(name = "cabecera_iibb", length = 80)
    private String cabeceraIibb;

    @Column(name = "cabecera_inicio_actividad", length = 40)
    private String cabeceraInicioActividad;

    /**
     * Si es true: "Razón social: Nombre".
     * Si es false: solo el nombre (sin etiqueta).
     */
    @Column(name = "cabecera_mostrar_etiqueta_razon_social", nullable = false)
    private Boolean cabeceraMostrarEtiquetaRazonSocial = true;

    @Column(name = "cabecera_mostrar_direccion", nullable = false)
    private Boolean cabeceraMostrarDireccion = true;

    @Column(name = "cabecera_mostrar_cuit", nullable = false)
    private Boolean cabeceraMostrarCuit = true;

    @Column(name = "cabecera_mostrar_condicion_iva", nullable = false)
    private Boolean cabeceraMostrarCondicionIva = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
