-- FASE 5: índices PostgreSQL (personas, ventas, créditos, finanzas)
-- Ejecutar manualmente en prod o vía PostgresPerformanceMigration al arrancar.

CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Lookups exactos
CREATE INDEX IF NOT EXISTS ix_cliente_dni ON cliente (dni) WHERE dni IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_empleado_dni ON empleado (dni) WHERE dni IS NOT NULL;

-- Ventas (historial + dashboard)
CREATE INDEX IF NOT EXISTS ix_venta_estado ON venta (estado);
CREATE INDEX IF NOT EXISTS ix_venta_sucursal_fecha_estado
    ON venta (sucursal_id, fecha_venta DESC, estado);

-- Créditos y cuentas
CREATE INDEX IF NOT EXISTS ix_credito_estado ON credito (estado);
CREATE INDEX IF NOT EXISTS ix_credito_cliente_estado ON credito (cliente_id, estado);
CREATE INDEX IF NOT EXISTS ix_credito_cuenta_sucursal_activo
    ON credito_cuenta (sucursal_id, activo) WHERE activo = true;
CREATE INDEX IF NOT EXISTS ix_credito_cuenta_cliente_sucursal
    ON credito_cuenta (cliente_id, sucursal_id);
CREATE INDEX IF NOT EXISTS ix_cuota_estado ON cuota (estado);
CREATE INDEX IF NOT EXISTS ix_cuota_credito_vencimiento ON cuota (credito_id, fecha_vencimiento);

-- Gastos, ODC, pagos, tesorería
CREATE INDEX IF NOT EXISTS ix_gasto_sucursal_estado_created
    ON gasto (sucursal_id, estado, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_odc_estado_fecha ON orden_de_compra (estado, fecha_orden DESC);
CREATE INDEX IF NOT EXISTS ix_pago_venta_estado ON pago_venta (estado);
CREATE INDEX IF NOT EXISTS ix_pago_venta_estado_venta
    ON pago_venta (estado, venta_id) WHERE estado = 'RECIBIDO';
CREATE INDEX IF NOT EXISTS ix_mov_financiero_cuenta_created
    ON movimiento_financiero (cuenta_id, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_caja_sesion_cuenta_estado
    ON caja_sesion (cuenta_id, estado) WHERE estado = 'ABIERTA';
CREATE INDEX IF NOT EXISTS ix_taxon_articulo_articulo_taxon
    ON taxon_articulo (articulo_id, taxon_id);

-- Búsqueda textual ILIKE '%q%'
CREATE INDEX IF NOT EXISTS ix_cliente_nombre_trgm
    ON cliente USING gin (lower(nombre) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS ix_cliente_apellido_trgm
    ON cliente USING gin (lower(apellido) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS ix_cliente_dni_trgm
    ON cliente USING gin (lower(dni) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS ix_cliente_nombre_completo_trgm
    ON cliente USING gin (lower(COALESCE(nombre, '') || ' ' || COALESCE(apellido, '')) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS ix_venta_numero_factura_trgm
    ON venta USING gin (lower(numero_factura) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS ix_proveedor_razon_social_trgm
    ON proveedor USING gin (lower(razon_social) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS ix_proveedor_codigo_trgm
    ON proveedor USING gin (lower(codigo) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS ix_gasto_descripcion_trgm
    ON gasto USING gin (lower(descripcion) gin_trgm_ops);

-- Vista materializada opcional: precio vigente (elimina LATERAL/subqueries repetidas)
-- CREATE MATERIALIZED VIEW IF NOT EXISTS mv_precio_vigente AS
-- SELECT DISTINCT ON (variante_articulo_id)
--     variante_articulo_id, precio_nuevo, costo_nuevo, fecha
-- FROM historial_precio
-- ORDER BY variante_articulo_id, fecha DESC;
-- CREATE UNIQUE INDEX IF NOT EXISTS ux_mv_precio_vigente_variante
--     ON mv_precio_vigente (variante_articulo_id);
