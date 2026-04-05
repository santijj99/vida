package com.vida.apirest.repositories;

import com.vida.apirest.model.almacen.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRepository extends JpaRepository<Stock, Long> {
}
