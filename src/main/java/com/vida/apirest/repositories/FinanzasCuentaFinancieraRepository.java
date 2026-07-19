package com.vida.apirest.repositories;

import com.vida.apirest.model.finanzas.CuentaFinanciera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FinanzasCuentaFinancieraRepository extends JpaRepository<CuentaFinanciera, Long> {
    Optional<CuentaFinanciera> findFirstByTipoAndActivoTrue(CuentaFinanciera.TipoCuenta tipo);
    List<CuentaFinanciera> findByTipoAndActivoTrue(CuentaFinanciera.TipoCuenta tipo);
    Optional<CuentaFinanciera> findByNumero(String numero);
    List<CuentaFinanciera> findBySucursalIdAndActivoTrueOrderByNombreAsc(Long sucursalId);

    @Query("""
            SELECT DISTINCT c.sucursal.id FROM FinanzasCuentaFinanciera c
            WHERE c.empleadoResponsable.id = :empleadoId AND c.activo = true
            """)
    List<Long> findDistinctSucursalIdsByEmpleadoResponsableId(@Param("empleadoId") Long empleadoId);
}
