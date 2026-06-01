package com.vida.apirest.dto.usuario;

import com.vida.apirest.dto.afip.TokenValidationResponse;
import lombok.Data;

@Data
public class LoginResponse {
    private String token;
    private UsuarioResponse usuario;
    private TokenValidationResponse afipToken;
}
