package com.vida.apirest.repositories;

import com.vida.apirest.model.articulo.Genero;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GeneroRepository extends JpaRepository<Genero, Long> {
}
