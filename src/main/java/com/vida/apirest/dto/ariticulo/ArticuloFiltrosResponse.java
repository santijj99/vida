package com.vida.apirest.dto.ariticulo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticuloFiltrosResponse {
    private List<String> categorias;
    private List<String> subCategorias;
    private List<String> generos;
    private List<String> marcas;
}
