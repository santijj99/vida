package com.vida.apirest.dto.pedido;

import lombok.Data;

import java.util.List;

@Data
public class ResolverVariantesRequest {
    private List<String> codigosBarras;
}
