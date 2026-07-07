package com.vida.apirest.dto.empresa;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmpresaAfipConfigResponse {
    private Long empresaId;
    private String empresaNombre;
    private String cuit;
    private String razonSocial;
    private String domicilio;
    private boolean afipHabilitado;
    private Integer ptoVta;
    private Integer cbteTipoDefault;
    private String condicionIva;
    private String iibb;
    private String inicioActividad;
    private String certificadosDirectorio;
    private boolean certificadosPresentes;
}
