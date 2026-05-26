package com.vida.apirest.dto.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ColumnasVistaResponse {
    private List<ColumnaVistaItem> columnasDisponibles;
    private List<String> columnasActivas;
}
