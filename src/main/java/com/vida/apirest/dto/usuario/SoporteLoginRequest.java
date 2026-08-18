package com.vida.apirest.dto.usuario;

import lombok.Data;

@Data
public class SoporteLoginRequest {

    private String token;
    /** Código de licencia de la empresa (requerido en modo multi-tenant). */
    private String codigoLicencia;
}
