package com.vida.apirest.dto.empresa;

import lombok.Data;

@Data
public class EmpresaAfipConfigRequest {
    private Boolean afipHabilitado;
    private Integer ptoVta;
    private Integer cbteTipoDefault;
    private String condicionIva;
    private String iibb;
    private String inicioActividad;
    private String certificadosDirectorio;
    private String clavePrivadaPassword;
}
