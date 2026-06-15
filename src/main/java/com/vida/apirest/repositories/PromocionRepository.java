package com.vida.apirest.repositories;

import com.vida.apirest.model.articulo.Promocion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PromocionRepository extends JpaRepository<Promocion, Long> {

    @Query("""
            SELECT DISTINCT p FROM Promocion p
            LEFT JOIN FETCH p.variantes
            ORDER BY p.createdAt DESC
            """)
    List<Promocion> findAllByOrderByCreatedAtDesc();

    @Query("""
            SELECT DISTINCT p FROM Promocion p
            LEFT JOIN FETCH p.variantes v
            LEFT JOIN FETCH v.variante var
            LEFT JOIN FETCH var.articulo a
            LEFT JOIN FETCH a.marca
            LEFT JOIN FETCH var.color
            LEFT JOIN FETCH var.talle
            WHERE p.id = :id
            """)
    Optional<Promocion> findByIdWithVariantes(@Param("id") Long id);
}
