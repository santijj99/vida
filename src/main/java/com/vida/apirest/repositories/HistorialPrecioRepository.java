package com.vida.apirest.repositories;

import com.vida.apirest.model.articulo.HistorialPrecio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HistorialPrecioRepository extends JpaRepository<HistorialPrecio, Long> {

    Optional<HistorialPrecio> findFirstByVarianteArticuloIdOrderByFechaDesc(Long varianteArticuloId);
}
