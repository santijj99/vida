package com.vida.apirest.dto.ariticulo;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticuloFiltrosResponse {
    private List<String> categorias;
    /** @deprecated usar {@link #clasificaciones} */
    private List<String> subCategorias;
    private List<String> clasificaciones;
    private List<String> generos;
    private List<String> marcas;
    private List<String> talles;
    private List<String> colores;
}
