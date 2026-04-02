package com.vida.apirest.repositories;

import com.vida.apirest.model.articulo.Articulo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticuloRepository extends JpaRepository<Articulo, Long> {
}
