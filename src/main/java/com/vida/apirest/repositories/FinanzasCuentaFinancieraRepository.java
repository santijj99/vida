package com.vida.apirest.repositories;

import com.vida.apirest.model.finanzas.CuentaFinanciera;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinanzasCuentaFinancieraRepository extends JpaRepository<CuentaFinanciera, Long> {
}
