package com.vida.apirest.dto.config;

import lombok.Data;

import java.util.List;

@Data
public class ColumnasVistaRequest {
    private List<String> columnas;
}
