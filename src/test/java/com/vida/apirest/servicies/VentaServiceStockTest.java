package com.vida.apirest.servicies;

import com.vida.apirest.model.almacen.Stock;
import com.vida.apirest.model.almacen.StockMovimiento;
import com.vida.apirest.model.articulo.Articulo;
import com.vida.apirest.repositories.StockRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VentaServiceStockTest {

    @Mock
    private StockRepository stockRepository;
    @Mock
    private StockOperacionesService stockOperacionesService;
    @InjectMocks
    private VentaService ventaService;

    @Test
    void rechazaSiNoHayDisponible() throws Exception {
        Stock stock = stockConDisponible(1);
        InvocationTargetException ex = assertThrows(
                InvocationTargetException.class,
                () -> ajustarStock(stock, 2));
        assertTrue(ex.getCause().getMessage().contains("stock suficiente"));
        verify(stockRepository, never()).save(any());
    }

    @Test
    void descuentaCuandoAlcanza() throws Exception {
        Stock stock = stockConDisponible(3);
        ajustarStock(stock, 2);
        assertEquals(1, stock.getCantidadDisponible());
        verify(stockRepository).save(stock);
        verify(stockOperacionesService).registrarMovimiento(
                eq(stock),
                eq(StockMovimiento.TipoMovimiento.SALIDA_VENTA),
                eq(2),
                eq(3),
                eq(1),
                eq("V-1"),
                any());
    }

    private void ajustarStock(Stock stock, int cantidad) throws Exception {
        Method method = VentaService.class.getDeclaredMethod("ajustarStock", Stock.class, Integer.class, String.class);
        method.setAccessible(true);
        method.invoke(ventaService, stock, cantidad, "V-1");
    }

    private static Stock stockConDisponible(int disponible) {
        Articulo articulo = new Articulo();
        articulo.setId(9L);
        Stock stock = new Stock();
        stock.setId(1L);
        stock.setArticulo(articulo);
        stock.setCantidadDisponible(disponible);
        stock.setCantidadActual(disponible);
        return stock;
    }
}
