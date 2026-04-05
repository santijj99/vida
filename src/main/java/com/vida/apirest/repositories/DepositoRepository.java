package com.vida.apirest.repositories;

import com.vida.apirest.model.almacen.Deposito;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepositoRepository extends JpaRepository<Deposito, Long> {
}
