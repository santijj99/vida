package com.vida.apirest.servicies;

import com.vida.apirest.dto.stock.StockResponse;
import com.vida.apirest.model.almacen.Stock;
import com.vida.apirest.repositories.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;

    @Transactional(readOnly = true)
    public List<StockResponse> listar() {
        return stockRepository.findAllWithRelations().stream().map(this::map).toList();
    }

    @Transactional
    public void eliminar(Long id) {
        if (!stockRepository.existsById(id)) {
            throw new RuntimeException("Stock no encontrado");
        }
        stockRepository.deleteById(id);
    }

    private StockResponse map(Stock stock) {
        return new StockResponse(
                stock.getId(),
                stock.getArticulo() != null ? stock.getArticulo().getId() : null,
                stock.getArticulo() != null ? stock.getArticulo().getCodigo() : null,
                stock.getVariante() != null ? stock.getVariante().getId() : null,
                stock.getVariante() != null ? stock.getVariante().getCodigoBarras() : null,
                stock.getSucursal() != null ? stock.getSucursal().getId() : null,
                stock.getSucursal() != null ? stock.getSucursal().getNombre() : null,
                stock.getCantidadActual(),
                stock.getCantidadDisponible(),
                stock.getCantidadReservada()
        );
    }
}
