package com.vida.apirest.repositories;

import com.vida.apirest.model.articulo.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    Optional<Categoria> findByNombre(String nombre);

    List<Categoria> findAllByOrderByNombreAsc();

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT c.nombre FROM Categoria c ORDER BY c.nombre")
    List<String> findDistinctNombres();
}
