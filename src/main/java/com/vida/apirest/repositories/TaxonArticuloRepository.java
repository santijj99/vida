package com.vida.apirest.repositories;

import com.vida.apirest.model.articulo.TaxonArticulo;
import com.vida.apirest.model.articulo.TaxonArticulo.TipoVinculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaxonArticuloRepository extends JpaRepository<TaxonArticulo, Long> {

    boolean existsByArticuloIdAndTaxonId(Long articuloId, Long taxonId);

    boolean existsByArticuloIdAndTaxonIdAndTipo(Long articuloId, Long taxonId, TipoVinculo tipo);

    List<TaxonArticulo> findByArticuloId(Long articuloId);

    List<TaxonArticulo> findByArticuloIdAndTipo(Long articuloId, TipoVinculo tipo);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM TaxonArticulo ta WHERE ta.articuloId = :articuloId AND ta.tipo = :tipo")
    void deleteByArticuloIdAndTipo(@Param("articuloId") Long articuloId, @Param("tipo") TipoVinculo tipo);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            DELETE FROM taxon_articulo ta
            WHERE NOT EXISTS (SELECT 1 FROM taxon t WHERE t.id = ta.taxon_id)
            """, nativeQuery = true)
    int deleteOrphanLinks();
}
