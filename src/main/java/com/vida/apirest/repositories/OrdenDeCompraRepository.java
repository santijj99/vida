package com.vida.apirest.repositories;

import com.vida.apirest.model.pedido.OrdenDeCompra;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrdenDeCompraRepository extends JpaRepository<OrdenDeCompra, Long> {

    @Query("""
            SELECT o FROM OrdenDeCompra o
            LEFT JOIN FETCH o.sucursal
            LEFT JOIN FETCH o.proveedor
            LEFT JOIN FETCH o.detalles d
            LEFT JOIN FETCH d.articulo a
            LEFT JOIN FETCH a.marca
            LEFT JOIN FETCH a.categoria
            LEFT JOIN FETCH a.genero
            LEFT JOIN FETCH d.variante v
            LEFT JOIN FETCH v.color
            LEFT JOIN FETCH v.talle
            WHERE o.id = :id
            """)
    Optional<OrdenDeCompra> findByIdWithRelations(@Param("id") Long id);

    @Query(
            value = """
                    SELECT o FROM OrdenDeCompra o
                    LEFT JOIN o.sucursal s
                    LEFT JOIN o.proveedor p
                    WHERE (:q IS NULL OR :q = '' OR
                           LOWER(o.numero) LIKE LOWER(CONCAT('%', :q, '%')) OR
                           LOWER(COALESCE(p.razonSocial, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
                           LOWER(COALESCE(p.codigo, '')) LIKE LOWER(CONCAT('%', :q, '%')))
                    AND (:estado IS NULL OR :estado = '' OR CAST(o.estado AS string) = :estado)
                    ORDER BY o.fechaOrden DESC, o.id DESC
                    """,
            countQuery = """
                    SELECT COUNT(o) FROM OrdenDeCompra o
                    LEFT JOIN o.proveedor p
                    WHERE (:q IS NULL OR :q = '' OR
                           LOWER(o.numero) LIKE LOWER(CONCAT('%', :q, '%')) OR
                           LOWER(COALESCE(p.razonSocial, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
                           LOWER(COALESCE(p.codigo, '')) LIKE LOWER(CONCAT('%', :q, '%')))
                    AND (:estado IS NULL OR :estado = '' OR CAST(o.estado AS string) = :estado)
                    """
    )
    Page<OrdenDeCompra> searchPage(
            @Param("q") String q,
            @Param("estado") String estado,
            Pageable pageable);
}
