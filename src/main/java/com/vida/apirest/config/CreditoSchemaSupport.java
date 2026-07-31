package com.vida.apirest.config;

import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * DDL de recargos de cuotas, reutilizable en DB default y en cada tenant.
 */
@Slf4j
public final class CreditoSchemaSupport {

    private CreditoSchemaSupport() {
    }

    public static void apply(DataSource dataSource) {
        try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
            st.execute("ALTER TABLE cuota ADD COLUMN IF NOT EXISTS recargo NUMERIC(15,2) DEFAULT 0");
            st.execute("ALTER TABLE cuota ADD COLUMN IF NOT EXISTS recargo_exento BOOLEAN DEFAULT FALSE");
            st.execute("ALTER TABLE cuota ADD COLUMN IF NOT EXISTS recargo_cobrado NUMERIC(15,2) DEFAULT 0");
            st.execute("UPDATE cuota SET recargo = 0 WHERE recargo IS NULL");
            st.execute("UPDATE cuota SET recargo_exento = FALSE WHERE recargo_exento IS NULL");
            st.execute("UPDATE cuota SET recargo_cobrado = 0 WHERE recargo_cobrado IS NULL");
            st.execute("ALTER TABLE pago_cuota ADD COLUMN IF NOT EXISTS monto_recargo NUMERIC(15,2) DEFAULT 0");
            st.execute("UPDATE pago_cuota SET monto_recargo = 0 WHERE monto_recargo IS NULL");
            log.info("DDL recargos de cuota aplicado");
        } catch (Exception e) {
            log.warn("DDL recargos de cuota (idempotente): {}", e.getMessage());
        }

        try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
            // Hibernate crea un CHECK sobre el enum; hay que ampliarlo al agregar modos nuevos.
            st.execute("""
                    ALTER TABLE credito_config_empresa
                    DROP CONSTRAINT IF EXISTS credito_config_empresa_modo_dia_vencimiento_check
                    """);
            st.execute("""
                    ALTER TABLE credito_config_empresa
                    ADD CONSTRAINT credito_config_empresa_modo_dia_vencimiento_check
                    CHECK (modo_dia_vencimiento IN (
                        'DIA_1','DIA_5','DIA_10','DIA_15','DIA_20',
                        'RANGO_1_10','RANGO_1_15','ULTIMO_MES','DIA_PERSONALIZADO'
                    ))
                    """);
            log.info("CHECK modo_dia_vencimiento actualizado");
        } catch (Exception e) {
            log.warn("CHECK modo_dia_vencimiento (idempotente): {}", e.getMessage());
        }
    }
}
