package com.vida.apirest.repositories;

import com.vida.apirest.model.finanzas.Moneda;
import com.vida.apirest.model.finanzas.TipoCambio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TipoCambioRepository extends JpaRepository<TipoCambio, Long> {

    Optional<TipoCambio> findByMonedaAndFecha(Moneda moneda, LocalDate fecha);

    List<TipoCambio> findByMonedaOrderByFechaDesc(Moneda moneda);

    List<TipoCambio> findByFechaOrderByMonedaAsc(LocalDate fecha);

    @Query("SELECT tc FROM TipoCambio tc WHERE tc.moneda = :moneda AND tc.fecha <= :fecha ORDER BY tc.fecha DESC")
    List<TipoCambio> findByMonedaAndFechaLessThanEqualOrderByFechaDesc(@Param("moneda") Moneda moneda, @Param("fecha") LocalDate fecha);

    @Query("SELECT tc FROM TipoCambio tc WHERE tc.fecha = :fecha")
    List<TipoCambio> findByFecha(@Param("fecha") LocalDate fecha);
}