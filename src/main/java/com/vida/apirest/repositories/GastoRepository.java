package com.vida.apirest.repositories;

import com.vida.apirest.model.finanzas.Gasto;
import com.vida.apirest.model.finanzas.Gasto.EstadoGasto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GastoRepository extends JpaRepository<Gasto, Long> {

    boolean existsByNumero(String numero);

    @Query(value = """
            SELECT DISTINCT g FROM Gasto g
            JOIN FETCH g.categoria
            JOIN FETCH g.sucursal
            WHERE (:sucursalId IS NULL OR g.sucursal.id = :sucursalId)
              AND (:estado IS NULL OR g.estado = :estado)
              AND (:categoriaId IS NULL OR g.categoria.id = :categoriaId)
              AND (
                :q IS NULL OR :q = '' OR
                LOWER(g.numero) LIKE LOWER(CONCAT('%', :q, '%')) OR
                LOWER(g.descripcion) LIKE LOWER(CONCAT('%', :q, '%')) OR
                LOWER(COALESCE(g.proveedor, '')) LIKE LOWER(CONCAT('%', :q, '%'))
              )
            """,
            countQuery = """
            SELECT COUNT(DISTINCT g) FROM Gasto g
            WHERE (:sucursalId IS NULL OR g.sucursal.id = :sucursalId)
              AND (:estado IS NULL OR g.estado = :estado)
              AND (:categoriaId IS NULL OR g.categoria.id = :categoriaId)
              AND (
                :q IS NULL OR :q = '' OR
                LOWER(g.numero) LIKE LOWER(CONCAT('%', :q, '%')) OR
                LOWER(g.descripcion) LIKE LOWER(CONCAT('%', :q, '%')) OR
                LOWER(COALESCE(g.proveedor, '')) LIKE LOWER(CONCAT('%', :q, '%'))
              )
            """)
    Page<Gasto> searchPage(
            @Param("sucursalId") Long sucursalId,
            @Param("estado") EstadoGasto estado,
            @Param("categoriaId") Long categoriaId,
            @Param("q") String q,
            Pageable pageable
    );

    @Query("""
            SELECT DISTINCT g FROM Gasto g
            JOIN FETCH g.categoria
            JOIN FETCH g.sucursal
            LEFT JOIN FETCH g.pagos p
            LEFT JOIN FETCH p.cuenta
            WHERE g.id = :id
            """)
    Optional<Gasto> findByIdWithRelations(@Param("id") Long id);
}
