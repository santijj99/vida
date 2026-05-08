package com.vida.apirest.repositories;

import com.vida.apirest.model.finanzas.CuentaFinanciera;
import com.vida.apirest.model.tesoreria.MovimientoFinanciero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MovimientoFinancieroRepository extends JpaRepository<MovimientoFinanciero, Long> {
    @Query("SELECT m FROM MovimientoFinanciero m WHERE m.cuenta.tipo = :tipo")
    List<MovimientoFinanciero> findByCuentaTipo(@Param("tipo") CuentaFinanciera.TipoCuenta tipo);
}
