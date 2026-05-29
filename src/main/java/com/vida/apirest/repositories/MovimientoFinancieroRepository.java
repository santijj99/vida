package com.vida.apirest.repositories;

import com.vida.apirest.model.finanzas.CuentaFinanciera;
import com.vida.apirest.model.tesoreria.MovimientoFinanciero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MovimientoFinancieroRepository extends JpaRepository<MovimientoFinanciero, Long> {
    @Query("SELECT m FROM MovimientoFinanciero m JOIN FETCH m.cuenta c WHERE c.tipo = :tipo ORDER BY m.createdAt DESC")
    List<MovimientoFinanciero> findByCuentaTipoOrderByCreatedAtDesc(@Param("tipo") CuentaFinanciera.TipoCuenta tipo);

    @Query("""
            SELECT m FROM MovimientoFinanciero m
            WHERE m.cuenta.id = :cuentaId
            AND m.createdAt >= :desde
            AND m.createdAt <= :hasta
            ORDER BY m.createdAt ASC
            """)
    List<MovimientoFinanciero> findByCuentaIdAndCreatedAtBetween(
            @Param("cuentaId") Long cuentaId,
            @Param("desde") java.time.LocalDateTime desde,
            @Param("hasta") java.time.LocalDateTime hasta);
}
