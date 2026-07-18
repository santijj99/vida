package com.vida.apirest.dto.usuario;

import lombok.Data;

@Data
public class LoginRequest {

    private String identificador;
    private String email;
    private String password;
    /** Código de licencia de la empresa (requerido en modo multi-tenant). */
    private String codigoLicencia;
}
