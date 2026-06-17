package com.vida.apirest.repositories;

import com.vida.apirest.model.finanzas.CuentaFinanciera;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FinanzasCuentaFinancieraRepository extends JpaRepository<CuentaFinanciera, Long> {
    Optional<CuentaFinanciera> findFirstByTipoAndActivoTrue(CuentaFinanciera.TipoCuenta tipo);
    List<CuentaFinanciera> findByTipoAndActivoTrue(CuentaFinanciera.TipoCuenta tipo);
    Optional<CuentaFinanciera> findByNumero(String numero);
    List<CuentaFinanciera> findBySucursalIdAndActivoTrueOrderByNombreAsc(Long sucursalId);
}
