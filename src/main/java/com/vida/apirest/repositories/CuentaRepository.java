package com.vida.apirest.repositories;

import com.vida.apirest.model.credito.Cuenta;
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
}
