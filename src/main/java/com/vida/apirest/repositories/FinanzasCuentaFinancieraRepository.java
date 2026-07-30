package com.vida.apirest.repositories;

import com.vida.apirest.model.finanzas.CuentaFinanciera;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FinanzasCuentaFinancieraRepository extends JpaRepository<CuentaFinanciera, Long> {
    Optional<CuentaFinanciera> findFirstByTipoAndActivoTrue(CuentaFinanciera.TipoCuenta tipo);
    List<CuentaFinanciera> findByTipoAndActivoTrue(CuentaFinanciera.TipoCuenta tipo);
    Optional<CuentaFinanciera> findByNumero(String numero);
    List<CuentaFinanciera> findBySucursalIdAndActivoTrueOrderByNombreAsc(Long sucursalId);

    /** Bloqueo pesimista para serializar ingresos/egresos sobre la misma cuenta. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM FinanzasCuentaFinanciera c WHERE c.id = :id")
    Optional<CuentaFinanciera> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            SELECT DISTINCT c.sucursal.id FROM FinanzasCuentaFinanciera c
            WHERE c.empleadoResponsable.id = :empleadoId AND c.activo = true
            """)
    List<Long> findDistinctSucursalIdsByEmpleadoResponsableId(@Param("empleadoId") Long empleadoId);
}
