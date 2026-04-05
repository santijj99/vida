package com.vida.apirest.repositories;

import com.vida.apirest.model.tesoreria.CuentaFinanciera;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TesoreriaCuentaFinancieraRepository extends JpaRepository<CuentaFinanciera, Long> {
}
