package com.vida.apirest.dto.usuario;

import lombok.Data;

@Data
public class CambiarPasswordInicialRequest {
    private String passwordActual;
    private String nuevaPassword;
}
