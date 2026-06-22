-- FASE 1: verificar tablas de módulos huérfanos antes de eliminar entidades JPA.
-- Ejecutar en PostgreSQL. Si COUNT = 0 en todas, es seguro borrar las entidades.

SELECT 'plan_cuentas' AS tabla, COUNT(*) AS filas FROM plan_cuentas
UNION ALL SELECT 'tercero', COUNT(*) FROM tercero
UNION ALL SELECT 'tercero_cuenta_corriente', COUNT(*) FROM tercero_cuenta_corriente
UNION ALL SELECT 'movimiento_cuenta_corriente', COUNT(*) FROM movimiento_cuenta_corriente
UNION ALL SELECT 'ingreso', COUNT(*) FROM ingreso
UNION ALL SELECT 'ingreso_categoria', COUNT(*) FROM ingreso_categoria
UNION ALL SELECT 'tesoreria_cuenta_financiera', COUNT(*) FROM tesoreria_cuenta_financiera
UNION ALL SELECT 'transferencia_financiera', COUNT(*) FROM transferencia_financiera
UNION ALL SELECT 'garante', COUNT(*) FROM garante
UNION ALL SELECT 'sub_categoria', COUNT(*) FROM sub_categoria
UNION ALL SELECT 'imagen', COUNT(*) FROM imagen
UNION ALL SELECT 'imagen_articulo', COUNT(*) FROM imagen_articulo
ORDER BY tabla;
