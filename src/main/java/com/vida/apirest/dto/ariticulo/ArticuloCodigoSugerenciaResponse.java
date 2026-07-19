package com.vida.apirest.dto.ariticulo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticuloCodigoSugerenciaResponse {
    private Long id;
    private String codigo;
    private String modelo;
    private String marca;
}
