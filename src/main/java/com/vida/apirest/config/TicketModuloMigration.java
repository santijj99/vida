package com.vida.apirest.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketModuloMigration implements ApplicationRunner {

    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) {
        TicketSchemaSupport.apply(dataSource);
        log.info("Migración configuración tickets aplicada (datasource default)");
    }
}
