package com.vida.apirest.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Aplica DDL/permisos de sueldos sobre el DataSource primario (dev / bootstrap).
 * En multi-tenant, cada DB de empresa se sincroniza en {@code TenantBootstrapService}.
 */
@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class SueldoModuloMigration implements ApplicationRunner {

    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) {
        SueldoSchemaSupport.apply(dataSource);
        RbacPermissionSyncSupport.syncCatalogoYAdmin(dataSource);
        log.info("Migración módulo sueldos aplicada (DataSource primario)");
    }
}
