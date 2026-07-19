package com.vida.apirest.servicies;

import com.vida.apirest.model.almacen.Stock;
import com.vida.apirest.model.almacen.StockMovimiento;
import com.vida.apirest.repositories.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockReservaService {

    private final StockRepository stockRepository;
    private final StockOperacionesService stockOperacionesService;

    public enum ModoReserva {
        CARRITO(
                StockMovimiento.TipoMovimiento.RESERVA_CARRITO,
                StockMovimiento.TipoMovimiento.LIBERACION_RESERVA_CARRITO,
                "Reserva por carrito pendiente",
                "Cancelación de carrito pendiente",
                "Cobro de carrito pendiente"
        ),
        PRESTAMO(
                StockMovimiento.TipoMovimiento.RESERVA_PRESTAMO,
                StockMovimiento.TipoMovimiento.LIBERACION_RESERVA_PRESTAMO,
                "Reserva por préstamo condicional",
                "Devolución de préstamo condicional",
                "Confirmación de compra por préstamo condicional"
        );

        private final StockMovimiento.TipoMovimiento tipoReserva;
        private final StockMovimiento.TipoMovimiento tipoLiberacion;
        private final String descripcionReserva;
        private final String descripcionLiberacion;
        private final String descripcionConsumo;

        ModoReserva(
                StockMovimiento.TipoMovimiento tipoReserva,
                StockMovimiento.TipoMovimiento tipoLiberacion,
                String descripcionReserva,
                String descripcionLiberacion,
                String descripcionConsumo
        ) {
            this.tipoReserva = tipoReserva;
            this.tipoLiberacion = tipoLiberacion;
            this.descripcionReserva = descripcionReserva;
            this.descripcionLiberacion = descripcionLiberacion;
            this.descripcionConsumo = descripcionConsumo;
        }
    }

    public void reservar(Stock stock, Integer cantidad, String referencia, ModoReserva modo) {
        int disponible = cantidadOZero(stock.getCantidadDisponible());
        if (disponible < cantidad) {
            throw new RuntimeException("Stock insuficiente para el artículo");
        }

        int actual = cantidadOZero(stock.getCantidadActual());
        int reservada = cantidadOZero(stock.getCantidadReservada());

        stock.setCantidadDisponible(disponible - cantidad);
        stock.setCantidadActual(Math.max(0, actual - cantidad));
        stock.setCantidadReservada(reservada + cantidad);
        stockRepository.save(stock);

        stockOperacionesService.registrarMovimiento(
                stock, modo.tipoReserva, cantidad, disponible, disponible - cantidad,
                referencia, modo.descripcionReserva);
    }

    public void liberar(Stock stock, Integer cantidad, String referencia, ModoReserva modo) {
        int reservada = cantidadOZero(stock.getCantidadReservada());
        if (reservada < cantidad) {
            throw new RuntimeException("Cantidad reservada insuficiente para liberar");
        }

        int disponible = cantidadOZero(stock.getCantidadDisponible());
        int actual = cantidadOZero(stock.getCantidadActual());

        stock.setCantidadReservada(reservada - cantidad);
        stock.setCantidadDisponible(disponible + cantidad);
        stock.setCantidadActual(actual + cantidad);
        stockRepository.save(stock);

        stockOperacionesService.registrarMovimiento(
                stock, modo.tipoLiberacion, cantidad, disponible, disponible + cantidad,
                referencia, modo.descripcionLiberacion);
    }

    public void consumir(Stock stock, Integer cantidad, String referencia, ModoReserva modo) {
        int reservada = cantidadOZero(stock.getCantidadReservada());
        if (reservada < cantidad) {
            throw new RuntimeException("Cantidad reservada insuficiente");
        }

        int disponible = cantidadOZero(stock.getCantidadDisponible());
        stock.setCantidadReservada(reservada - cantidad);
        stockRepository.save(stock);

        stockOperacionesService.registrarMovimiento(
                stock, StockMovimiento.TipoMovimiento.SALIDA_VENTA, cantidad,
                disponible, disponible, referencia, modo.descripcionConsumo);
    }

    private static int cantidadOZero(Integer value) {
        return value != null ? value : 0;
    }
}
