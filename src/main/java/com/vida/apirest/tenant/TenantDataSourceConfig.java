package com.vida.apirest.tenant;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.HashMap;

@Configuration
public class TenantDataSourceConfig {

    @Bean
    public HikariDataSource defaultDataSource(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password,
            @Value("${spring.datasource.driver-class-name:org.postgresql.Driver}") String driver
    ) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driver);
        config.setPoolName("default");
        return new HikariDataSource(config);
    }

    @Bean
    @Primary
    public DataSource dataSource(
            HikariDataSource defaultDataSource,
            ObjectProvider<TenantDataSourceManager> tenantDataSourceManager
    ) {
        TenantRoutingDataSource routing = new TenantRoutingDataSource(tenantDataSourceManager);
        routing.setDefaultDataSource(defaultDataSource);
        routing.setTargetDataSources(new HashMap<>());
        routing.setLenientFallback(true);
        routing.afterPropertiesSet();
        return routing;
    }
}
