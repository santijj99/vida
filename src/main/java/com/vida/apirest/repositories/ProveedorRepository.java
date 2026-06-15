package com.vida.apirest.repositories;

import com.vida.apirest.model.persona.Proveedor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    Optional<Proveedor> findByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, Long id);

    @Query(
            value = """
                    SELECT p FROM Proveedor p
                    WHERE (:soloActivos = false OR p.activo = true)
                    AND (:q IS NULL OR :q = '' OR
                         LOWER(COALESCE(p.codigo, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
                         LOWER(COALESCE(p.razonSocial, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
                         LOWER(COALESCE(p.nombre, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
                         LOWER(COALESCE(p.cuitCuil, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
                         LOWER(COALESCE(p.email, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
                         LOWER(COALESCE(p.telefono, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
                         LOWER(COALESCE(p.domicilio, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
                         LOWER(COALESCE(p.ciudad, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
                         LOWER(COALESCE(p.provincia, '')) LIKE LOWER(CONCAT('%', :q, '%')))
                    """,
            countQuery = """
                    SELECT COUNT(p) FROM Proveedor p
                    WHERE (:soloActivos = false OR p.activo = true)
                    AND (:q IS NULL OR :q = '' OR
                         LOWER(COALESCE(p.codigo, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
                         LOWER(COALESCE(p.razonSocial, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
                         LOWER(COALESCE(p.nombre, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
                         LOWER(COALESCE(p.cuitCuil, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
                         LOWER(COALESCE(p.email, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
                         LOWER(COALESCE(p.telefono, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
                         LOWER(COALESCE(p.domicilio, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
                         LOWER(COALESCE(p.ciudad, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
                         LOWER(COALESCE(p.provincia, '')) LIKE LOWER(CONCAT('%', :q, '%')))
                    """
    )
    Page<Proveedor> searchPage(
            @Param("q") String q,
            @Param("soloActivos") boolean soloActivos,
            Pageable pageable);
}
