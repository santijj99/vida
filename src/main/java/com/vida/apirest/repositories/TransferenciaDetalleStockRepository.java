package com.vida.apirest.repositories;

import com.vida.apirest.model.almacen.TransferenciaDetalleStock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferenciaDetalleStockRepository extends JpaRepository<TransferenciaDetalleStock, Long> {
}
