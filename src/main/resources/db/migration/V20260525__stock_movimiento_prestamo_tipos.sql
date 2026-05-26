-- Amplía el CHECK de tipo en stock_movimiento para préstamos condicionales.
-- Ejecutar manualmente si no usás el migrador al arranque.

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
    'LIBERACION_RESERVA_PRESTAMO'
));
