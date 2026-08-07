package com.vida.apirest.tenant;

import com.vida.apirest.config.LicenciaProperties;
import com.vida.apirest.exception.ForbiddenException;
import com.vida.apirest.servicies.licencia.DeviceUuidResolver;
import com.vida.apirest.servicies.licencia.LicenciaServerClient;
import com.vida.apirest.servicies.licencia.LicenciaServerClient.ValidacionRemotaResult;
import com.vida.apirest.tenant.TenantConnectionCacheStore.CachedTenantConnection;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantDataSourceManager {

    private final LicenciaProperties properties;
    private final LicenciaServerClient licenciaServerClient;
    private final TenantAesDecryptor aesDecryptor;
    private final DeviceUuidResolver deviceUuidResolver;
    private final TenantConnectionCacheStore connectionCacheStore;
    /** Lazy: evita ciclo Manager → Bootstrap → JPA → DataSource → Manager */
    private final ObjectProvider<TenantBootstrapService> tenantBootstrapService;

    private final Map<String, HikariDataSource> pools = new ConcurrentHashMap<>();
    /** licencia|uuid → momento a partir del cual hay que revalidar el dispositivo. */
    private final Map<String, Instant> proximaValidacionDispositivo = new ConcurrentHashMap<>();

    @PostConstruct
    void logMode() {
        if (isMultiTenantEnabled()) {
            log.warn("MULTI-TENANT ACTIVO → cada request usa la DB de la licencia (X-Licencia-Codigo). "
                    + "Servidor licencias: {}. Revalidación/gracia: {} días",
                    properties.getServerUrl(), properties.getGraciaDias());
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
        registrarDispositivo(codigo);
    }

    /**
     * El pool se cachea por licencia, así que la validación de {@code createPool}
     * solo corre para el primer equipo que entra. Sin esto los demás equipos
     * nunca llegan a aparecer en el servidor de licencias.
     */
    private void registrarDispositivo(String codigo) {
        String deviceUuid = deviceUuidResolver.resolve();
        String key = codigo + "|" + deviceUuid;
        Instant proxima = proximaValidacionDispositivo.get(key);
        if (proxima != null && Instant.now().isBefore(proxima)) {
            return;
        }

        // Si la última validación OK todavía está dentro de la ventana de gracia,
        // no hace falta pegarle al servidor de licencias en cada login.
        CachedTenantConnection cached = connectionCacheStore.find(codigo).orElse(null);
        if (cached != null && dentroDeGracia(cached.ultimoExito())) {
            proximaValidacionDispositivo.put(key, cached.ultimoExito().plus(ventanaGracia()));
            return;
        }

        ValidacionRemotaResult result = licenciaServerClient.validar(
                codigo, deviceUuid, deviceUuidResolver.resolveNombre());
        if (!result.isAlcanzable()) {
            if (cached != null && dentroDeGracia(cached.ultimoExito())) {
                proximaValidacionDispositivo.put(key, Instant.now().plus(Duration.ofHours(6)));
                return;
            }
            throw new ForbiddenException(
                    "No se pudo contactar el servidor de licencias y el período de gracia venció. "
                            + result.getMensaje());
        }
        if (!result.isValida()) {
            proximaValidacionDispositivo.remove(key);
            throw new ForbiddenException(
                    result.getMensaje() != null ? result.getMensaje() : "Licencia inválida");
        }
        Instant now = Instant.now();
        guardarCache(codigo, deviceUuid, result, now);
        proximaValidacionDispositivo.put(key, now.plus(ventanaGracia()));
        log.info("Dispositivo revalidado en licencias: licencia={} uuid={}", codigo, deviceUuid);
    }

    private HikariDataSource createPool(String codigo) {
        String deviceUuid = deviceUuidResolver.resolve();
        CachedTenantConnection cached = connectionCacheStore.find(codigo).orElse(null);

        // Dentro de la ventana: arrancar con la última conexión OK sin exigir internet.
        if (cached != null && dentroDeGracia(cached.ultimoExito()) && hasConnection(cached)) {
            log.info("Usando caché de conexión (gracia {} días) para licencia={}",
                    properties.getGraciaDias(), codigo);
            HikariDataSource ds = openPool(codigo, cached.getHost(), cached.getPuerto(),
                    cached.getDatabaseName(), cached.getUsername(), cached.getPasswordEncriptada(),
                    cached.getSsl());
            proximaValidacionDispositivo.put(codigo + "|" + deviceUuid,
                    cached.ultimoExito().plus(ventanaGracia()));
            // Si hay red, refrescar en background no es trivial aquí;
            // el próximo ciclo tras vencer la ventana forzará revalidación.
            return ds;
        }

        ValidacionRemotaResult result = licenciaServerClient.validar(
                codigo, deviceUuid, deviceUuidResolver.resolveNombre());

        if (!result.isAlcanzable()) {
            if (cached != null && dentroDeGracia(cached.ultimoExito()) && hasConnection(cached)) {
                log.warn("Servidor de licencias inalcanzable; usando caché en gracia para licencia={}",
                        codigo);
                HikariDataSource ds = openPool(codigo, cached.getHost(), cached.getPuerto(),
                        cached.getDatabaseName(), cached.getUsername(), cached.getPasswordEncriptada(),
                        cached.getSsl());
                proximaValidacionDispositivo.put(codigo + "|" + deviceUuid,
                        Instant.now().plus(Duration.ofHours(6)));
                return ds;
            }
            throw new ForbiddenException(
                    "No se pudo contactar el servidor de licencias: " + result.getMensaje()
                            + ". Si ya validaste antes, el período de gracia es de "
                            + properties.getGraciaDias() + " días.");
        }
        if (!result.isValida()) {
            throw new ForbiddenException(
                    result.getMensaje() != null ? result.getMensaje() : "Licencia inválida");
        }
        if (!result.hasConnection()) {
            throw new ForbiddenException(
                    "La empresa no tiene conexión de base de datos configurada en el servidor de licencias");
        }

        Instant now = Instant.now();
        guardarCache(codigo, deviceUuid, result, now);

        HikariDataSource ds = openPool(codigo, result.getHost(), result.getPuerto(),
                result.getDatabaseName(), result.getUsername(), result.getPasswordEncriptada(),
                result.getSsl());
        proximaValidacionDispositivo.put(codigo + "|" + deviceUuid, now.plus(ventanaGracia()));
        return ds;
    }

    private HikariDataSource openPool(
            String codigo,
            String host,
            Integer puerto,
            String databaseName,
            String username,
            String passwordEncriptada,
            Boolean sslFlag
    ) {
        String password = aesDecryptor.decrypt(passwordEncriptada);
        boolean ssl = sslFlag == null || sslFlag;
        String jdbcUrl = String.format(
                "jdbc:postgresql://%s:%d/%s%s",
                host,
                puerto,
                databaseName,
                ssl ? "?sslmode=require" : ""
        );

        HikariConfig config = new HikariConfig();
        config.setPoolName("tenant-" + codigo);
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
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
                codigo, databaseName, host, puerto);

        tenantBootstrapService.getObject().bootstrapIfNeeded(ds, codigo);
        return ds;
    }

    private void guardarCache(
            String codigo,
            String deviceUuid,
            ValidacionRemotaResult result,
            Instant now
    ) {
        CachedTenantConnection cached = new CachedTenantConnection();
        cached.setCodigoLicencia(codigo);
        cached.setDeviceUuid(deviceUuid);
        cached.setUltimoExito(now);
        cached.setHost(result.getHost());
        cached.setPuerto(result.getPuerto());
        cached.setDatabaseName(result.getDatabaseName());
        cached.setUsername(result.getUsername());
        cached.setPasswordEncriptada(result.getPasswordEncriptada());
        cached.setSsl(result.getSsl());
        cached.setEmpresaNombre(result.getEmpresaNombre());
        cached.setPlanNombre(result.getPlanNombre());
        connectionCacheStore.save(cached);
    }

    private boolean dentroDeGracia(Instant ultimoExito) {
        if (ultimoExito == null) {
            return false;
        }
        return Instant.now().isBefore(ultimoExito.plus(ventanaGracia()));
    }

    private Duration ventanaGracia() {
        return Duration.ofDays(Math.max(0, properties.getGraciaDias()));
    }

    private static boolean hasConnection(CachedTenantConnection c) {
        return c.getHost() != null && !c.getHost().isBlank()
                && c.getDatabaseName() != null && !c.getDatabaseName().isBlank()
                && c.getUsername() != null && !c.getUsername().isBlank()
                && c.getPasswordEncriptada() != null && !c.getPasswordEncriptada().isBlank()
                && c.getPuerto() != null;
    }
}
