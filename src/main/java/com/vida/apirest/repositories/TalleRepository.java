package com.vida.apirest.repositories;

import com.vida.apirest.model.articulo.Talle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TalleRepository extends JpaRepository<Talle, Long> {
    Optional<Talle> findByPaisAndNumero(Talle.Pais pais, String numero);
}
