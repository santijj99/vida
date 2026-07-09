package com.vida.apirest.dto.usuario;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String email;
    private String codigo;
    private String nuevaPassword;
}
