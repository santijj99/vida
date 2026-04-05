package com.vida.apirest.repositories;

import com.vida.apirest.model.credito.Credito;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditoRepository extends JpaRepository<Credito, Long> {
}
