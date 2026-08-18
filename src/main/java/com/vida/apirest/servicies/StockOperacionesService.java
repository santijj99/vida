package com.vida.apirest.servicies;

import com.vida.apirest.exception.ResourceNotFoundException;
import com.vida.apirest.model.almacen.Stock;
import com.vida.apirest.model.almacen.StockMovimiento;
import com.vida.apirest.repositories.StockMovimientoRepository;
import com.vida.apirest.repositories.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class StockOperacionesService {

    private final StockRepository stockRepository;
    private final StockMovimientoRepository stockMovimientoRepository;

    @PersistenceContext
    private EntityManager entityManager;

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

    /**
     * Relee la fila con {@code SELECT … FOR UPDATE} para que dos cobros
     * simultáneos no pasen el tope (B-01).
     */
    public Stock lockById(Long id) {
        if (id == null) {
            throw new ResourceNotFoundException("Stock no encontrado");
        }
        Stock locked = stockRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stock no encontrado"));
        entityManager.refresh(locked, LockModeType.PESSIMISTIC_WRITE);
        return locked;
    }

    public Stock lock(Stock stock) {
        if (stock == null || stock.getId() == null) {
            throw new ResourceNotFoundException("Stock no encontrado");
        }
        return lockById(stock.getId());
    }

    public Stock requireStockForUpdate(Long articuloId, Long varianteId, Long sucursalId) {
        return lock(requireStock(articuloId, varianteId, sucursalId));
    }

    public Stock requireStockByVarianteForUpdate(Long varianteId, Long sucursalId) {
        return lock(requireStockByVariante(varianteId, sucursalId));
    }

    /** Toma locks en orden de id para no deadlockear ventas cruzadas. */
    public Map<Long, Stock> lockAllById(Collection<Long> ids) {
        Map<Long, Stock> locked = new LinkedHashMap<>();
        ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .forEach(id -> locked.put(id, lockById(id)));
        return locked;
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
