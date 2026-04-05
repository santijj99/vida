package com.vida.apirest.repositories;

import com.vida.apirest.model.tesoreria.TransferenciaFinanciera;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferenciaFinancieraRepository extends JpaRepository<TransferenciaFinanciera, Long> {
}
