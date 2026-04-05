package com.vida.apirest.repositories;

import com.vida.apirest.model.almacen.StockMovimiento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovimientoRepository extends JpaRepository<StockMovimiento, Long> {
}
