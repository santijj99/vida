-- orden_de_compra.proveedor_id debe referenciar proveedor(id), no tercero(id)
ALTER TABLE orden_de_compra DROP CONSTRAINT IF EXISTS fk2egrdb5ykft1clcu8r621ey3m;

DO $body$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint c
    JOIN pg_class t ON c.conrelid = t.oid
    JOIN pg_class ref ON c.confrelid = ref.oid
    WHERE t.relname = 'orden_de_compra'
      AND c.conname = 'fk_odc_proveedor'
      AND c.contype = 'f'
      AND ref.relname = 'proveedor'
  ) THEN
    ALTER TABLE orden_de_compra
      ADD CONSTRAINT fk_odc_proveedor
      FOREIGN KEY (proveedor_id) REFERENCES proveedor(id);
  END IF;
END $body$;
