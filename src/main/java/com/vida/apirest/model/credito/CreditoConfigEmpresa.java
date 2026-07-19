package com.vida.apirest.model.credito;

import com.vida.apirest.model.empresa.Empresa;
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
        name = "credito_config_empresa",
        uniqueConstraints = @UniqueConstraint(name = "uk_credito_config_empresa", columnNames = "empresa_id")
)
public class CreditoConfigEmpresa {

    public enum TipoInteresMora { FIJO, ACUMULATIVO }

    /** Día fijo del mes (1, 5, 10, 15, 20) o último día. */
    public enum ModoDiaVencimiento {
        DIA_1, DIA_5, DIA_10, DIA_15, DIA_20,
        RANGO_1_10, RANGO_1_15,
        ULTIMO_MES
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false, unique = true)
    private Empresa empresa;

    @Column(name = "dias_gracia", nullable = false)
    private Integer diasGracia = 0;

    @Column(name = "porcentaje_mora", nullable = false, precision = 9, scale = 4)
    private BigDecimal porcentajeMora = BigDecimal.TEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_interes", nullable = false, length = 20)
    private TipoInteresMora tipoInteres = TipoInteresMora.FIJO;

    @Enumerated(EnumType.STRING)
    @Column(name = "modo_dia_vencimiento", nullable = false, length = 30)
    private ModoDiaVencimiento modoDiaVencimiento = ModoDiaVencimiento.DIA_10;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
