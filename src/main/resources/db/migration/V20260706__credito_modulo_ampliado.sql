-- Módulo créditos ampliado (ejecutar manualmente en prod si ddl-auto: none)
-- También aplicado por CreditoModuloMigration en arranque.

ALTER TABLE cuota ADD COLUMN IF NOT EXISTS recargo NUMERIC(15,2) DEFAULT 0;
ALTER TABLE cuota ADD COLUMN IF NOT EXISTS recargo_exento BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS credito_config_empresa (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL UNIQUE REFERENCES empresa(id),
    dias_gracia INTEGER NOT NULL DEFAULT 0,
    porcentaje_mora NUMERIC(9,4) NOT NULL DEFAULT 10,
    tipo_interes VARCHAR(20) NOT NULL DEFAULT 'FIJO',
    modo_dia_vencimiento VARCHAR(30) NOT NULL DEFAULT 'DIA_10',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS credito_historial (
    id BIGSERIAL PRIMARY KEY,
    credito_id BIGINT NOT NULL REFERENCES credito(id),
    campo VARCHAR(100) NOT NULL,
    valor_anterior TEXT,
    valor_nuevo TEXT,
    usuario_id BIGINT,
    usuario_nombre VARCHAR(200),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS ix_credito_historial_credito
    ON credito_historial (credito_id, created_at DESC);
