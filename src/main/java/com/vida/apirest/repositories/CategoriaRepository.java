package com.vida.apirest.repositories;

import com.vida.apirest.model.articulo.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
}
