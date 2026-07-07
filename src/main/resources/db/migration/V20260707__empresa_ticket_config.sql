-- Configuración de formato PDF de tickets por empresa
CREATE TABLE IF NOT EXISTS empresa_ticket_config (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL UNIQUE REFERENCES empresa(id),
    formato VARCHAR(20) NOT NULL DEFAULT 'TERMICO_80MM',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
