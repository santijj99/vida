package com.vida.apirest.dto.usuario;

import lombok.Data;

@Data
public class LoginRequest {

    private String email;
    private String password;
}
