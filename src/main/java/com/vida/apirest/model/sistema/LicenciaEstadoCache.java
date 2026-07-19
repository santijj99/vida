package com.vida.apirest.model.sistema;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "licencia_estado_cache")
@Getter
@Setter
public class LicenciaEstadoCache {

    @Id
    private Long id = 1L;

    @Column(nullable = false)
    private boolean valida = false;

    @Column(length = 40)
    private String estado;

    @Column(length = 80)
    private String codigoError;

    @Column(length = 500)
    private String mensaje;

    @Column(length = 200)
    private String empresaNombre;

    @Column(length = 100)
    private String planNombre;

    private LocalDate fechaVencimiento;

    private Integer cantidadMaximaDispositivos;

    private Integer cantidadMaximaSucursales;

    @Column(length = 64)
    private String deviceUuid;

    /** Última consulta al servidor (éxito o error de negocio). */
    private Instant ultimaValidacion;

    /** Última vez que el servidor respondió licencia válida. */
    private Instant ultimoExito;

    /** true si el último intento no pudo contactar al servidor. */
    private boolean servidorInalcanzable = false;

    private boolean modoGracia = false;
}
