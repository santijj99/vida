package com.vida.apirest.repositories;

import com.vida.apirest.model.almacen.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findByArticuloIdAndVarianteIdAndSucursalId(Long articuloId, Long varianteId, Long sucursalId);
    Optional<Stock> findByArticuloIdAndSucursalId(Long articuloId, Long sucursalId);
    Optional<Stock> findByVarianteIdAndSucursalId(Long varianteId, Long sucursalId);
}
