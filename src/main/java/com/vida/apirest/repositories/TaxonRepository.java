package com.vida.apirest.repositories;

import com.vida.apirest.model.articulo.Taxon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaxonRepository extends JpaRepository<Taxon, Long> {
    Optional<Taxon> findByNombre(String nombre);

    @org.springframework.data.jpa.repository.Query("""
            SELECT DISTINCT t.nombre FROM Taxon t
            WHERE EXISTS (
                SELECT 1 FROM TaxonArticulo ta WHERE ta.taxonId = t.id
            )
            ORDER BY t.nombre
            """)
    List<String> findDistinctNombresUsadosEnArticulos();
}
