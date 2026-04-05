package com.vida.apirest.model.empresa;

import com.vida.apirest.model.finanzas.Moneda;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "configuracion_empresa",
        indexes = @Index(name = "ix_config_empresa", columnList = "empresa_id")
)
public class ConfiguracionEmpresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(name = "clave_configuracion", nullable = false, length = 100)
    private String claveConfiguracion;

    @Column(name = "valor", columnDefinition = "TEXT")
    private String valor;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "tipo", length = 50)
    private String tipo; // STRING, NUMBER, BOOLEAN, DATE
}
