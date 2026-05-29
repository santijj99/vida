package com.vida.apirest.repositories;

import com.vida.apirest.model.articulo.Marca;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MarcaRepository extends JpaRepository<Marca, Long> {
    Optional<Marca> findByNombre(String nombre);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT m.nombre FROM Marca m ORDER BY m.nombre")
    List<String> findDistinctNombres();
}
