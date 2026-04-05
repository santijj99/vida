package com.vida.apirest.repositories;

import com.vida.apirest.model.credito.Cuota;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CuotaRepository extends JpaRepository<Cuota, Long> {
}
