package com.vida.apirest.dto.ariticulo;

import lombok.Data;

@Data
public class CreateTalleRequest {
    private String pais;
    private String numero;
    private String descripcion;
}
