package com.vida.apirest.repositories;

import com.vida.apirest.model.articulo.Articulo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ArticuloRepository extends JpaRepository<Articulo, Long>, JpaSpecificationExecutor<Articulo> {

    Optional<Articulo> findByCodigo(String codigo);

    @Query("""
            SELECT DISTINCT a FROM Articulo a
            LEFT JOIN FETCH a.marca
            LEFT JOIN FETCH a.categoria
            LEFT JOIN FETCH a.genero
            LEFT JOIN FETCH a.variantes v
            LEFT JOIN FETCH v.color
            LEFT JOIN FETCH v.talle
            WHERE a.id = :id
            """)
    Optional<Articulo> findByIdWithVariantes(@Param("id") Long id);

    @Query("""
            SELECT DISTINCT a FROM Articulo a
            LEFT JOIN FETCH a.taxones ta
            LEFT JOIN FETCH ta.taxon
            WHERE a.id = :id
            """)
    Optional<Articulo> findByIdWithTaxones(@Param("id") Long id);

    @Query("""
            SELECT DISTINCT a FROM Articulo a
            LEFT JOIN FETCH a.marca
            LEFT JOIN FETCH a.categoria
            WHERE a.estado = com.vida.apirest.model.articulo.Articulo.EstadoProducto.ARCHIVADO
            """)
    List<Articulo> findArchivadosWithCatalogo();
}
