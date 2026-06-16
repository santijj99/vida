package com.vida.apirest.repositories;

import com.vida.apirest.model.almacen.Deposito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepositoRepository extends JpaRepository<Deposito, Long> {
    Optional<Deposito> findByCodigo(String codigo);
    List<Deposito> findBySucursalIdOrderByNombreAsc(Long sucursalId);
}
