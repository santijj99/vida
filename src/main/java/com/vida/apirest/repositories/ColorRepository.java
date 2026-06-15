package com.vida.apirest.repositories;

import com.vida.apirest.model.articulo.Color;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ColorRepository extends JpaRepository<Color, Long> {
    Optional<Color> findByNombre(String nombre);

    List<Color> findAllByOrderByNombreAsc();

    @Query("SELECT DISTINCT c.nombre FROM Color c ORDER BY c.nombre ASC")
    List<String> findDistinctNombres();
}
