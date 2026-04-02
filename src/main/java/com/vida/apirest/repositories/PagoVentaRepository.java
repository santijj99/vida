package com.vida.apirest.repositories;

import com.vida.apirest.model.venta.PagoVenta;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PagoVentaRepository extends JpaRepository<PagoVenta, Long> {
}
