package com.vida.apirest.repositories;

import com.vida.apirest.model.almacen.TransferenciaDeStock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferenciaDeStockRepository extends JpaRepository<TransferenciaDeStock, Long> {
}
