package com.vida.apirest.servicies;

import com.vida.apirest.exception.ResourceNotFoundException;
import com.vida.apirest.model.almacen.Stock;
import com.vida.apirest.model.articulo.Articulo;
import com.vida.apirest.repositories.StockMovimientoRepository;
import com.vida.apirest.repositories.StockRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockOperacionesServiceTest {

    @Mock
    private StockRepository stockRepository;
    @Mock
    private StockMovimientoRepository stockMovimientoRepository;
    @Mock
    private EntityManager entityManager;
    @InjectMocks
    private StockOperacionesService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "entityManager", entityManager);
    }

    @Test
    void lockAllByIdTomaLosIdsEnOrden() {
        Stock s1 = stock(1L);
        Stock s7 = stock(7L);
        Stock s3 = stock(3L);
        when(stockRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(s1));
        when(stockRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(s3));
        when(stockRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(s7));

        Map<Long, Stock> locked = service.lockAllById(List.of(7L, 1L, 3L, 1L));

        InOrder order = inOrder(stockRepository, entityManager);
        order.verify(stockRepository).findByIdForUpdate(1L);
        order.verify(entityManager).refresh(s1, LockModeType.PESSIMISTIC_WRITE);
        order.verify(stockRepository).findByIdForUpdate(3L);
        order.verify(entityManager).refresh(s3, LockModeType.PESSIMISTIC_WRITE);
        order.verify(stockRepository).findByIdForUpdate(7L);
        order.verify(entityManager).refresh(s7, LockModeType.PESSIMISTIC_WRITE);
        assertEquals(3, locked.size());
        assertSame(s1, locked.get(1L));
    }

    @Test
    void lockByIdFaltaLanzaNotFound() {
        when(stockRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.lockById(99L));
    }

    private static Stock stock(Long id) {
        Stock stock = new Stock();
        stock.setId(id);
        Articulo articulo = new Articulo();
        articulo.setId(id);
        stock.setArticulo(articulo);
        return stock;
    }
}
