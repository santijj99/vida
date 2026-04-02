package com.vida.apirest.repositories;

import com.vida.apirest.model.venta.Venta;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VentaRepository extends JpaRepository<Venta, Long> {
}
