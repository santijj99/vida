package com.vida.apirest.repositories;

import com.vida.apirest.model.credito.Cuenta;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CuentaRepository extends JpaRepository<Cuenta, Long> {
}
