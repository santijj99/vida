-- Idempotencia de sync offline: mismo client_request_id = misma venta (sin duplicar).
ALTER TABLE venta
    ADD COLUMN IF NOT EXISTS client_request_id VARCHAR(96);

CREATE UNIQUE INDEX IF NOT EXISTS uk_venta_client_request_id
    ON venta (client_request_id);
