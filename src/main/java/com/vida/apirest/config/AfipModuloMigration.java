package com.vida.apirest.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Debe correr antes que {@link DataInitializer} (Order 1+), que consulta empresa_afip_config.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class AfipModuloMigration implements ApplicationRunner {

    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) {
        AfipSchemaSupport.apply(dataSource);
        log.info("Migración módulo AFIP aplicada");
    }
}
