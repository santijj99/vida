package com.vida.apirest.repositories;

import com.vida.apirest.model.articulo.Imagen;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImagenRepository extends JpaRepository<Imagen, Long> {
}
