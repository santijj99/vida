package com.vida.apirest.repositories;

import com.vida.apirest.model.finanzas.CajaSesion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CajaSesionRepository extends JpaRepository<CajaSesion, Long> {

    @Query("""
            SELECT s FROM CajaSesion s
            JOIN FETCH s.cuenta c
            WHERE c.id = :cuentaId AND s.estado = :estado
            """)
    Optional<CajaSesion> findAbiertaByCuentaId(
            @Param("cuentaId") Long cuentaId,
            @Param("estado") CajaSesion.EstadoSesion estado);

    @Query("""
            SELECT s FROM CajaSesion s
            JOIN FETCH s.cuenta c
            WHERE c.id = :cuentaId
            ORDER BY s.fechaApertura DESC
            """)
    List<CajaSesion> findByCuentaIdOrderByFechaAperturaDesc(@Param("cuentaId") Long cuentaId);

    @Query("""
            SELECT s FROM CajaSesion s
            JOIN FETCH s.cuenta c
            WHERE s.id = :id
            """)
    Optional<CajaSesion> findByIdWithCuenta(@Param("id") Long id);
}
