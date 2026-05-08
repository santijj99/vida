package com.vida.apirest.repositories;

import com.vida.apirest.model.articulo.Taxon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TaxonRepository extends JpaRepository<Taxon, Long> {
    Optional<Taxon> findByNombre(String nombre);
}
