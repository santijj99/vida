package com.vida.apirest.repositories;

import com.vida.apirest.model.articulo.Genero;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GeneroRepository extends JpaRepository<Genero, Long> {
    Optional<Genero> findByNombre(String nombre);

    List<Genero> findAllByOrderByNombreAsc();

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT g.nombre FROM Genero g ORDER BY g.nombre")
    List<String> findDistinctNombres();
}
