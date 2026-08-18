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
        name = "empresa_afip_config",
        indexes = @Index(name = "ix_empresa_afip_config_empresa", columnList = "empresa_id", unique = true)
)
public class EmpresaAfipConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false, unique = true)
    private Empresa empresa;

    @Column(name = "afip_habilitado", nullable = false)
    private boolean afipHabilitado = false;

    @Column(name = "pto_vta", nullable = false)
    private Integer ptoVta = 1;

    @Column(name = "cbte_tipo_default", nullable = false)
    private Integer cbteTipoDefault = 6;

    @Column(name = "condicion_iva", length = 120)
    private String condicionIva = "IVA Responsable Inscripto";

    @Column(name = "iibb", length = 50)
    private String iibb;

    @Column(name = "inicio_actividad", length = 20)
    private String inicioActividad;

    /**
     * Directorio con certificado.crt, MiClavePrivada.key y TA.xml.
     * Si es null, se usa {afip.certificados-base-dir}/{empresaId}/.
     */
    @Column(name = "certificados_directorio", length = 500)
    private String certificadosDirectorio;

    /** Password PKCS#12/PEM cifrada ({@code AESGCM:} + payload). Legado: texto plano. */
    @Column(name = "clave_privada_password", length = 512)
    private String clavePrivadaPassword;

    /**
     * true = homologación (testing); false = producción.
     * Se recuerda por empresa al cambiar el ambiente en ARCA.
     */
    @Column(name = "homologacion", nullable = false)
    private boolean homologacion = true;

    /**
     * Copia del último TA.xml (wsfe) generado por Java, por si se pierde el archivo en disco.
     */
    @Column(name = "ta_xml_wsfe", columnDefinition = "TEXT")
    private String taXmlWsfe;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
