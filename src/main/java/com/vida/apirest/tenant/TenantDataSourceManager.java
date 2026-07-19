package com.vida.apirest.tenant;

import com.vida.apirest.config.LicenciaProperties;
import com.vida.apirest.exception.ForbiddenException;
import com.vida.apirest.servicies.licencia.LicenciaServerClient;
import com.vida.apirest.servicies.licencia.LicenciaServerClient.ValidacionRemotaResult;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantDataSourceManager {

    private static final Path DEVICE_UUID_PATH = Path.of("data", "device-uuid.txt");

    private final LicenciaProperties properties;
    private final LicenciaServerClient licenciaServerClient;
    private final TenantAesDecryptor aesDecryptor;
    /** Lazy: evita ciclo Manager → Bootstrap → JPA → DataSource → Manager */
    private final ObjectProvider<TenantBootstrapService> tenantBootstrapService;

    private final Map<String, HikariDataSource> pools = new ConcurrentHashMap<>();

    @PostConstruct
    void logMode() {
        if (isMultiTenantEnabled()) {
            log.warn("MULTI-TENANT ACTIVO → cada request usa la DB de la licencia (X-Licencia-Codigo). "
                    + "Servidor licencias: {}", properties.getServerUrl());
        } else {
            log.warn("MULTI-TENANT OFF → se usa solo spring.datasource (DB local). "
                    + "Para activar: app.licencia.enabled=true y multi-tenant=true");
        }
    }

    public boolean isMultiTenantEnabled() {
        return properties.isEnabled() && properties.isMultiTenant();
    }

    public DataSource resolve(String codigoLicencia) {
        String codigo = codigoLicencia == null ? "" : codigoLicencia.trim();
        if (codigo.isEmpty()) {
            throw new ForbiddenException("Falta el código de licencia (header X-Licencia-Codigo)");
        }
        return pools.computeIfAbsent(codigo, this::createPool);
    }

    public void ensureTenantReady(String codigoLicencia) {
        if (!isMultiTenantEnabled()) {
            return;
        }
        String codigo = codigoLicencia == null ? "" : codigoLicencia.trim();
        if (codigo.isEmpty()) {
            throw new ForbiddenException("Debés indicar el código de licencia de la empresa");
        }
        resolve(codigo);
    }

    private HikariDataSource createPool(String codigo) {
        String deviceUuid = resolveDeviceUuid();
        ValidacionRemotaResult result = licenciaServerClient.validar(codigo, deviceUuid);

        if (!result.isAlcanzable()) {
            throw new ForbiddenException(
                    "No se pudo contactar el servidor de licencias: " + result.getMensaje());
        }
        if (!result.isValida()) {
            throw new ForbiddenException(
                    result.getMensaje() != null ? result.getMensaje() : "Licencia inválida");
        }
        if (!result.hasConnection()) {
            throw new ForbiddenException(
                    "La empresa no tiene conexión de base de datos configurada en el servidor de licencias");
        }

        String password = aesDecryptor.decrypt(result.getPasswordEncriptada());
        boolean ssl = result.getSsl() == null || result.getSsl();
        // Neon y la mayoría de cloud exigen sslmode=require
        String jdbcUrl = String.format(
                "jdbc:postgresql://%s:%d/%s%s",
                result.getHost(),
                result.getPuerto(),
                result.getDatabaseName(),
                ssl ? "?sslmode=require" : ""
        );

        HikariConfig config = new HikariConfig();
        config.setPoolName("tenant-" + codigo);
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(result.getUsername());
        config.setPassword(password);
        config.setDriverClassName("org.postgresql.Driver");
        config.setMaximumPoolSize(8);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(20_000);
        config.setMaxLifetime(300_000);

        HikariDataSource ds;
        try {
            ds = new HikariDataSource(config);
        } catch (Exception ex) {
            String detail = ex.getMessage() == null ? "" : ex.getMessage();
            if (detail.toLowerCase().contains("password") || detail.toLowerCase().contains("authentication")) {
                throw new ForbiddenException(
                        "Neon rechazó usuario/password de la conexión. "
                                + "En admin_licencias → Conexiones, editá la conexión y pegá de nuevo "
                                + "la password actual de Neon (solo la password, no la URL completa). "
                                + "Detalle: " + detail);
            }
            throw new ForbiddenException("No se pudo conectar a la DB del tenant: " + detail);
        }
        log.info("Pool multi-tenant creado para licencia={} db={}@{}:{}",
                codigo, result.getDatabaseName(), result.getHost(), result.getPuerto());

        tenantBootstrapService.getObject().bootstrapIfNeeded(ds, codigo);
        return ds;
    }

    private String resolveDeviceUuid() {
        String configured = properties.getDeviceUuid();
        if (configured != null && !configured.isBlank()) {
            return configured.trim();
        }
        try {
            if (Files.exists(DEVICE_UUID_PATH)) {
                String existing = Files.readString(DEVICE_UUID_PATH).trim();
                if (!existing.isBlank()) {
                    return existing;
                }
            }
            Files.createDirectories(DEVICE_UUID_PATH.getParent());
            String generated = UUID.randomUUID().toString();
            Files.writeString(DEVICE_UUID_PATH, generated);
            return generated;
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }
}
