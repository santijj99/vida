-- Ítems solicitados al proveedor pueden no existir aún en catálogo
ALTER TABLE orden_de_compra_detalle ALTER COLUMN articulo_id DROP NOT NULL;
ALTER TABLE orden_de_compra_detalle ALTER COLUMN variante_id DROP NOT NULL;
