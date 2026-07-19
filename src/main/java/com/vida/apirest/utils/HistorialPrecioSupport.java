package com.vida.apirest.utils;

import com.vida.apirest.model.articulo.HistorialPrecio;
import com.vida.apirest.repositories.HistorialPrecioRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class HistorialPrecioSupport {

    private HistorialPrecioSupport() {
    }

    public static void registrarCambio(
            HistorialPrecioRepository repository,
            Long varianteArticuloId,
            BigDecimal precio,
            BigDecimal costo
    ) {
        HistorialPrecio historial = new HistorialPrecio();
        historial.setVarianteArticuloId(varianteArticuloId);
        historial.setPrecioNuevo(precio);
        if (costo != null && costo.compareTo(BigDecimal.ZERO) >= 0) {
            historial.setCostoNuevo(costo);
        }
        historial.setFecha(LocalDateTime.now());
        repository.save(historial);
    }
}
