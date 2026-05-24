package com.vida.apirest.dto.cliente;

import lombok.Data;

@Data
public class DireccionRequest {
    private String pais;
    private String provincia;
    private String localidad;
    private String barrio;
    private String calle;
    private String numero;
    private String observacion;
}
