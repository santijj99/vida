package com.vida.apirest.dto.afip;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class TokenValidationResponse {
    private boolean activo;
    private String mensaje;
    private Date expirationTime;
    private boolean regenerado;
}
