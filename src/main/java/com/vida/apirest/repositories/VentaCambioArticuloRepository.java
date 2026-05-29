package com.vida.apirest.repositories;

import com.vida.apirest.model.venta.VentaCambioArticulo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VentaCambioArticuloRepository extends JpaRepository<VentaCambioArticulo, Long> {
    List<VentaCambioArticulo> findByVentaIdOrderByCreatedAtDesc(Long ventaId);
}
