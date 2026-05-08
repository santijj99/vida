package com.vida.apirest.repositories;

import com.vida.apirest.model.credito.Cuenta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CuentaRepository extends JpaRepository<Cuenta, Long> {
    Optional<Cuenta> findByClienteIdAndSucursalIdAndActivoTrue(Long clienteId, Long sucursalId);
}
