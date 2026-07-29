package com.vida.apirest.dto.sistema;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Builder
public class SistemaInfoResponse {

    private String aplicacion;
    private String version;
    private boolean licenciaHabilitada;
    private boolean licenciaValida;
    private boolean modoGracia;
    private boolean servidorInalcanzable;
    private String estado;
    private String codigoError;
    private String mensaje;
    private String empresaNombre;
    private String planNombre;
    private LocalDate fechaVencimiento;
    private Integer cantidadMaximaDispositivos;
    private Integer cantidadMaximaSucursales;
    /** Código usado en la validación (enmascarado). */
    private String codigoLicencia;
    private String deviceUuid;
    private Instant ultimaValidacion;
    private Instant ultimoExito;
}
