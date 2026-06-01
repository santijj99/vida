package com.vida.apirest.repositories;

import com.vida.apirest.model.credito.Cuenta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CuentaRepository extends JpaRepository<Cuenta, Long> {
    Optional<Cuenta> findByClienteIdAndSucursalIdAndActivoTrue(Long clienteId, Long sucursalId);

    @Query("SELECT c FROM Cuenta c JOIN FETCH c.cliente JOIN FETCH c.sucursal WHERE c.activo = true ORDER BY c.saldoActual DESC")
    List<Cuenta> findAllActivasWithCliente();

    @Query("SELECT c FROM Cuenta c JOIN FETCH c.cliente JOIN FETCH c.sucursal WHERE c.activo = true AND c.sucursal.id = :sucursalId ORDER BY c.saldoActual DESC")
    List<Cuenta> findActivasBySucursalWithCliente(@Param("sucursalId") Long sucursalId);

    @Query("SELECT c FROM Cuenta c JOIN FETCH c.cliente JOIN FETCH c.sucursal WHERE c.id = :id")
    Optional<Cuenta> findByIdWithCliente(@Param("id") Long id);

    @Query("SELECT c FROM Cuenta c JOIN FETCH c.cliente JOIN FETCH c.sucursal WHERE c.cliente.id = :clienteId AND c.activo = true")
    Optional<Cuenta> findActivaByClienteId(@Param("clienteId") Long clienteId);

    @EntityGraph(attributePaths = {"cliente", "sucursal"})
    @Query(
            value = """
                    SELECT c FROM Cuenta c
                    JOIN c.cliente cl
                    JOIN c.sucursal s
                    WHERE c.activo = true
                    AND (:sucursalId IS NULL OR s.id = :sucursalId)
                    AND (:q IS NULL OR :q = '' OR
                          LOWER(cl.nombre) LIKE LOWER(CONCAT('%', :q, '%')) OR
                          LOWER(cl.apellido) LIKE LOWER(CONCAT('%', :q, '%')) OR
                          LOWER(cl.dni) LIKE LOWER(CONCAT('%', :q, '%')) OR
                          LOWER(CONCAT(COALESCE(cl.nombre, ''), ' ', COALESCE(cl.apellido, ''))) LIKE LOWER(CONCAT('%', :q, '%')) OR
                          LOWER(c.numero) LIKE LOWER(CONCAT('%', :q, '%')) OR
                          LOWER(s.nombre) LIKE LOWER(CONCAT('%', :q, '%')))
                    AND (
                        :estadoCredito IS NULL OR :estadoCredito = '' OR :estadoCredito = 'TODOS' OR
                        (:estadoCredito = 'ACTIVO' AND EXISTS (
                            SELECT 1 FROM Credito cr
                            WHERE cr.cliente.id = cl.id
                            AND cr.estado = com.vida.apirest.model.credito.Credito.EstadoCredito.ACTIVO
                        )) OR
                        (:estadoCredito = 'VENCIDO' AND EXISTS (
                            SELECT 1 FROM Credito cr
                            WHERE cr.cliente.id = cl.id
                            AND cr.estado = com.vida.apirest.model.credito.Credito.EstadoCredito.VENCIDO
                        ))
                    )
                    """,
            countQuery = """
                    SELECT COUNT(c) FROM Cuenta c
                    JOIN c.cliente cl
                    JOIN c.sucursal s
                    WHERE c.activo = true
                    AND (:sucursalId IS NULL OR s.id = :sucursalId)
                    AND (:q IS NULL OR :q = '' OR
                          LOWER(cl.nombre) LIKE LOWER(CONCAT('%', :q, '%')) OR
                          LOWER(cl.apellido) LIKE LOWER(CONCAT('%', :q, '%')) OR
                          LOWER(cl.dni) LIKE LOWER(CONCAT('%', :q, '%')) OR
                          LOWER(CONCAT(COALESCE(cl.nombre, ''), ' ', COALESCE(cl.apellido, ''))) LIKE LOWER(CONCAT('%', :q, '%')) OR
                          LOWER(c.numero) LIKE LOWER(CONCAT('%', :q, '%')) OR
                          LOWER(s.nombre) LIKE LOWER(CONCAT('%', :q, '%')))
                    AND (
                        :estadoCredito IS NULL OR :estadoCredito = '' OR :estadoCredito = 'TODOS' OR
                        (:estadoCredito = 'ACTIVO' AND EXISTS (
                            SELECT 1 FROM Credito cr
                            WHERE cr.cliente.id = cl.id
                            AND cr.estado = com.vida.apirest.model.credito.Credito.EstadoCredito.ACTIVO
                        )) OR
                        (:estadoCredito = 'VENCIDO' AND EXISTS (
                            SELECT 1 FROM Credito cr
                            WHERE cr.cliente.id = cl.id
                            AND cr.estado = com.vida.apirest.model.credito.Credito.EstadoCredito.VENCIDO
                        ))
                    )
                    """
    )
    Page<Cuenta> searchPage(
            @Param("sucursalId") Long sucursalId,
            @Param("q") String q,
            @Param("estadoCredito") String estadoCredito,
            Pageable pageable
    );
}
