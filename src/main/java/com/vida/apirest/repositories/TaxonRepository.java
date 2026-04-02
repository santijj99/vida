package com.vida.apirest.repositories;

import com.vida.apirest.model.articulo.Taxon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxonRepository extends JpaRepository<Taxon, Long> {
}
