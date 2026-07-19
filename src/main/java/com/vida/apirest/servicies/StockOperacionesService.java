package com.vida.apirest.servicies;

import com.vida.apirest.exception.ResourceNotFoundException;
import com.vida.apirest.model.almacen.Stock;
import com.vida.apirest.model.almacen.StockMovimiento;
import com.vida.apirest.repositories.StockMovimientoRepository;
import com.vida.apirest.repositories.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockOperacionesService {

    private final StockRepository stockRepository;
    private final StockMovimientoRepository stockMovimientoRepository;

    @Transactional(readOnly = true)
    public Stock requireStock(Long articuloId, Long varianteId, Long sucursalId) {
        if (varianteId != null) {
            return stockRepository
                    .findFirstByArticuloIdAndVarianteIdAndSucursalIdOrderByIdAsc(articuloId, varianteId, sucursalId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Stock no encontrado para el artículo/variante en la sucursal"));
        }
        return stockRepository.findFirstByArticuloIdAndSucursalIdOrderByIdAsc(articuloId, sucursalId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stock no encontrado para el artículo en la sucursal"));
    }

    @Transactional(readOnly = true)
    public Stock requireStockByVariante(Long varianteId, Long sucursalId) {
        return stockRepository.findFirstByVarianteIdAndSucursalIdOrderByIdAsc(varianteId, sucursalId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stock no encontrado para la variante en la sucursal"));
    }

    @Transactional
    public void registrarMovimiento(
            Stock stock,
            StockMovimiento.TipoMovimiento tipo,
            int cantidad,
            int saldoAnterior,
            int saldoNuevo,
            String referencia,
            String descripcion
    ) {
        StockMovimiento movimiento = new StockMovimiento();
        movimiento.setStock(stock);
        movimiento.setTipo(tipo);
        movimiento.setCantidad(cantidad);
        movimiento.setSaldoAnterior(saldoAnterior);
        movimiento.setSaldoNuevo(saldoNuevo);
        movimiento.setReferencia(referencia);
        movimiento.setDescripcion(descripcion);
        movimiento.setUsuario("sistema");
        stockMovimientoRepository.save(movimiento);
    }
}
