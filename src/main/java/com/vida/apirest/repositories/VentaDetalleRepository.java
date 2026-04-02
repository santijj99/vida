package com.vida.apirest.repositories;

import com.vida.apirest.model.venta.VentaDetalle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VentaDetalleRepository extends JpaRepository<VentaDetalle, Long> {
}
