package com.vida.apirest.dto.usuario;

import lombok.Data;

@Data
public class LoginResponse {
    private  String token;
    private UsuarioResponse usuario;
}
