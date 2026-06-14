package com.vida.apirest.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.vida.apirest.model.articulo.VarianteArticulo;

public interface VarianteArticuloRepository extends JpaRepository<VarianteArticulo, Long> {

    boolean existsByCodigoBarras(String codigoBarras);

    boolean existsByArticuloIdAndColorIdAndTalleIdAndEstado(
            Long articuloId,
            Long colorId,
            Long talleId,
            VarianteArticulo.EstadoVariante estado);

    Optional<VarianteArticulo> findByArticuloIdAndColorIdAndTalleIdAndEstado(
            Long articuloId,
            Long colorId,
            Long talleId,
            VarianteArticulo.EstadoVariante estado);

    @Query("""
            SELECT v FROM VarianteArticulo v
            JOIN FETCH v.articulo a
            LEFT JOIN FETCH a.marca
            LEFT JOIN FETCH a.categoria
            LEFT JOIN FETCH a.genero
            LEFT JOIN FETCH v.color
            LEFT JOIN FETCH v.talle
            WHERE v.codigoBarras = :codigo
            """)
    Optional<VarianteArticulo> findByCodigoBarrasWithRelations(@Param("codigo") String codigo);

    @Query("""
            SELECT v FROM VarianteArticulo v
            JOIN FETCH v.articulo a
            LEFT JOIN FETCH a.marca
            LEFT JOIN FETCH a.categoria
            LEFT JOIN FETCH a.genero
            LEFT JOIN FETCH v.color
            LEFT JOIN FETCH v.talle
            WHERE v.id = :id
            """)
    Optional<VarianteArticulo> findByIdWithRelations(@Param("id") Long id);

    @Query("""
            SELECT v FROM VarianteArticulo v
            JOIN FETCH v.articulo a
            LEFT JOIN FETCH a.marca
            LEFT JOIN FETCH a.categoria
            LEFT JOIN FETCH a.genero
            LEFT JOIN FETCH v.color
            LEFT JOIN FETCH v.talle
            WHERE v.codigoBarras IN :codigos
            """)
    List<VarianteArticulo> findByCodigoBarrasIn(@Param("codigos") List<String> codigos);
}
