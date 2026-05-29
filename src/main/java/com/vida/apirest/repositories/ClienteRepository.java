package com.vida.apirest.repositories;

import com.vida.apirest.model.persona.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByDni(String dni);

    @EntityGraph(attributePaths = {"direccion", "garante", "contactos"})
    @Query(
            value = """
                    SELECT DISTINCT c FROM Cliente c
                    LEFT JOIN c.direccion d
                    LEFT JOIN c.garante g
                    WHERE :q IS NULL OR :q = '' OR
                          LOWER(c.nombre) LIKE LOWER(CONCAT('%', :q, '%')) OR
                          LOWER(c.apellido) LIKE LOWER(CONCAT('%', :q, '%')) OR
                          LOWER(c.dni) LIKE LOWER(CONCAT('%', :q, '%')) OR
                          LOWER(CONCAT(COALESCE(c.nombre, ''), ' ', COALESCE(c.apellido, ''))) LIKE LOWER(CONCAT('%', :q, '%')) OR
                          LOWER(CONCAT(COALESCE(g.nombre, ''), ' ', COALESCE(g.apellido, ''))) LIKE LOWER(CONCAT('%', :q, '%')) OR
                          LOWER(COALESCE(d.calle, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
                          LOWER(COALESCE(d.localidad, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
                          LOWER(COALESCE(d.barrio, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
                          LOWER(COALESCE(d.provincia, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                    """,
            countQuery = """
                    SELECT COUNT(DISTINCT c.id) FROM Cliente c
                    LEFT JOIN c.direccion d
                    LEFT JOIN c.garante g
                    WHERE :q IS NULL OR :q = '' OR
                          LOWER(c.nombre) LIKE LOWER(CONCAT('%', :q, '%')) OR
                          LOWER(c.apellido) LIKE LOWER(CONCAT('%', :q, '%')) OR
                          LOWER(c.dni) LIKE LOWER(CONCAT('%', :q, '%')) OR
                          LOWER(CONCAT(COALESCE(c.nombre, ''), ' ', COALESCE(c.apellido, ''))) LIKE LOWER(CONCAT('%', :q, '%')) OR
                          LOWER(CONCAT(COALESCE(g.nombre, ''), ' ', COALESCE(g.apellido, ''))) LIKE LOWER(CONCAT('%', :q, '%')) OR
                          LOWER(COALESCE(d.calle, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
                          LOWER(COALESCE(d.localidad, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
                          LOWER(COALESCE(d.barrio, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
                          LOWER(COALESCE(d.provincia, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                    """
    )
    Page<Cliente> searchPage(@Param("q") String q, Pageable pageable);
}
