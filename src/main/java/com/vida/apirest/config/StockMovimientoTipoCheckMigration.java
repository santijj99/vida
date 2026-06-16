package com.vida.apirest.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * PostgreSQL mantiene un CHECK en stock_movimiento.tipo que Hibernate no actualiza
 * al agregar valores al enum Java. Se recrea al iniciar la aplicación.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockMovimientoTipoCheckMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    private static final String SQL = """
            ALTER TABLE stock_movimiento DROP CONSTRAINT IF EXISTS stock_movimiento_tipo_check;
            ALTER TABLE stock_movimiento ADD CONSTRAINT stock_movimiento_tipo_check CHECK (tipo IN (
                'INGRESO_COMPRA',
                'INGRESO_DEVOLUCION',
                'INGRESO_AJUSTE',
                'SALIDA_VENTA',
                'SALIDA_DEVOLUCION',
                'SALIDA_AJUSTE',
                'SALIDA_TRANSFERENCIA',
                'INGRESO_TRANSFERENCIA',
                'SALIDA_MERMA',
                'SALIDA_OBSEQUIO',
                'RESERVA_PRESTAMO',
                'LIBERACION_RESERVA_PRESTAMO',
                'RESERVA_CARRITO',
                'LIBERACION_RESERVA_CARRITO'
            ));
            """;

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute(SQL);
            log.info("Constraint stock_movimiento_tipo_check actualizado (tipos de stock movimiento).");
        } catch (Exception e) {
            log.warn("No se pudo actualizar stock_movimiento_tipo_check: {}", e.getMessage());
        }
    }
}
