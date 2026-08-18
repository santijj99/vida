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
import jakarta.annotation.PreDestroy;
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

    private final Map<String, ManagedPool> pools = new ConcurrentHashMap<>();
    /** licencia|uuid → momento a partir del cual hay que revalidar (TTL online, no gracia). */
    private final Map<String, Instant> proximaValidacionDispositivo = new ConcurrentHashMap<>();

    @PostConstruct
    void logMode() {
        if (isMultiTenantEnabled()) {
            log.warn("MULTI-TENANT ACTIVO → cada request usa la DB de la licencia (X-Licencia-Codigo). "
                            + "Servidor licencias: {}. Revalidación online: {} min. Gracia offline: {} días",
                    properties.getServerUrl(),
                    properties.getCacheMinutos(),
                    properties.getGraciaDias());
        } else {
            log.warn("MULTI-TENANT OFF → se usa solo spring.datasource (DB local). "
                    + "Para activar: app.licencia.enabled=true y multi-tenant=true");
        }
    }

    @PreDestroy
    void shutdownPools() {
        pools.keySet().forEach(this::evict);
    }

    public boolean isMultiTenantEnabled() {
        return properties.isEnabled() && properties.isMultiTenant();
    }

    public DataSource resolve(String codigoLicencia) {
        String codigo = codigoLicencia == null ? "" : codigoLicencia.trim();
        if (codigo.isEmpty()) {
            throw new ForbiddenException("Falta el código de licencia (header X-Licencia-Codigo)");
        }
        ManagedPool current = pools.get(codigo);
        if (current != null && current.isOpen()) {
            return current.ds;
        }
        return pools.compute(codigo, (k, prev) -> {
            if (prev != null && prev.isOpen()) {
                return prev;
            }
            return createManagedPool(k);
        }).ds;
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

    public void evict(String codigoLicencia) {
        String codigo = codigoLicencia == null ? "" : codigoLicencia.trim();
        if (codigo.isEmpty()) {
            return;
        }
        ManagedPool removed = pools.remove(codigo);
        closeQuietly(removed);
        String prefix = codigo + "|";
        proximaValidacionDispositivo.keySet().removeIf(k -> k.equals(codigo) || k.startsWith(prefix));
        log.info("Pool multi-tenant cerrado para licencia={}", codigo);
    }

    /**
     * El pool se cachea por licencia, así que la validación de {@code createManagedPool}
     * solo corre para el primer equipo. Sin esto los demás equipos nunca llegan a
     * aparecer en el servidor de licencias. TTL corto si hay red; gracia solo si está caído.
     */
    private void registrarDispositivo(String codigo) {
        String deviceUuid = deviceUuidResolver.resolve();
        String key = codigo + "|" + deviceUuid;
        Instant proxima = proximaValidacionDispositivo.get(key);
        if (proxima != null && Instant.now().isBefore(proxima)) {
            return;
        }

        CachedTenantConnection cached = connectionCacheStore.find(codigo).orElse(null);
        ValidacionRemotaResult result = licenciaServerClient.validar(
                codigo, deviceUuid, deviceUuidResolver.resolveNombre());

        if (!result.isAlcanzable()) {
            if (cached != null && dentroDeGraciaOffline(cached.ultimoExito())) {
                proximaValidacionDispositivo.put(key, Instant.now().plus(properties.reintentoSiServidorCaido()));
                log.warn("Licencias inalcanzable; gracia offline para licencia={}", codigo);
                return;
            }
            evict(codigo);
            throw new ForbiddenException(
                    "No se pudo contactar el servidor de licencias y el período de gracia venció. "
                            + result.getMensaje());
        }
        if (!result.isValida()) {
            rechazarYCerrar(codigo, key, result.getMensaje());
        }
        Instant now = Instant.now();
        guardarCache(codigo, deviceUuid, result, now);
        syncPoolFromRemote(codigo, result);
        proximaValidacionDispositivo.put(key, now.plus(properties.revalidacionOnline()));
        log.info("Dispositivo revalidado en licencias: licencia={} uuid={}", codigo, deviceUuid);
    }

    private ManagedPool createManagedPool(String codigo) {
        String deviceUuid = deviceUuidResolver.resolve();
        CachedTenantConnection cached = connectionCacheStore.find(codigo).orElse(null);

        ValidacionRemotaResult result = licenciaServerClient.validar(
                codigo, deviceUuid, deviceUuidResolver.resolveNombre());

        if (!result.isAlcanzable()) {
            if (cached != null && dentroDeGraciaOffline(cached.ultimoExito()) && hasConnection(cached)) {
                log.warn("Servidor de licencias inalcanzable; usando caché en gracia para licencia={}",
                        codigo);
                HikariDataSource ds = openPool(codigo, cached.getHost(), cached.getPuerto(),
                        cached.getDatabaseName(), cached.getUsername(), cached.getPasswordEncriptada(),
                        cached.getSsl());
                proximaValidacionDispositivo.put(codigo + "|" + deviceUuid,
                        Instant.now().plus(properties.reintentoSiServidorCaido()));
                return new ManagedPool(ds, fingerprint(
                        cached.getHost(), cached.getPuerto(), cached.getDatabaseName(),
                        cached.getUsername(), cached.getPasswordEncriptada(), cached.getSsl()));
            }
            throw new ForbiddenException(
                    "No se pudo contactar el servidor de licencias: " + result.getMensaje()
                            + ". Si ya validaste antes, el período de gracia es de "
                            + properties.getGraciaDias() + " días.");
        }
        if (!result.isValida()) {
            connectionCacheStore.delete(codigo);
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
        proximaValidacionDispositivo.put(codigo + "|" + deviceUuid, now.plus(properties.revalidacionOnline()));
        return new ManagedPool(ds, fingerprint(result));
    }

    private void syncPoolFromRemote(String codigo, ValidacionRemotaResult result) {
        if (!result.hasConnection()) {
            throw new ForbiddenException(
                    "La empresa no tiene conexión de base de datos configurada en el servidor de licencias");
        }
        String fp = fingerprint(result);
        ManagedPool current = pools.get(codigo);
        if (current != null && current.isOpen() && fp.equals(current.fingerprint)) {
            return;
        }
        HikariDataSource ds = openPool(codigo, result.getHost(), result.getPuerto(),
                result.getDatabaseName(), result.getUsername(), result.getPasswordEncriptada(),
                result.getSsl());
        ManagedPool prev = pools.put(codigo, new ManagedPool(ds, fp));
        closeQuietly(prev);
        log.info("Pool multi-tenant recreado (conexión actualizada) para licencia={}", codigo);
    }

    private void rechazarYCerrar(String codigo, String deviceKey, String mensaje) {
        proximaValidacionDispositivo.remove(deviceKey);
        connectionCacheStore.delete(codigo);
        evict(codigo);
        throw new ForbiddenException(mensaje != null && !mensaje.isBlank() ? mensaje : "Licencia inválida");
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

    private boolean dentroDeGraciaOffline(Instant ultimoExito) {
        if (ultimoExito == null) {
            return false;
        }
        Duration gracia = properties.graciaOffline();
        if (gracia.isZero() || gracia.isNegative()) {
            return false;
        }
        return Instant.now().isBefore(ultimoExito.plus(gracia));
    }

    private static String fingerprint(ValidacionRemotaResult result) {
        return fingerprint(
                result.getHost(), result.getPuerto(), result.getDatabaseName(),
                result.getUsername(), result.getPasswordEncriptada(), result.getSsl());
    }

    private static String fingerprint(
            String host, Integer puerto, String databaseName,
            String username, String passwordEncriptada, Boolean ssl
    ) {
        return String.join("|",
                String.valueOf(host),
                String.valueOf(puerto),
                String.valueOf(databaseName),
                String.valueOf(username),
                String.valueOf(passwordEncriptada),
                String.valueOf(ssl));
    }

    private static boolean hasConnection(CachedTenantConnection c) {
        return c.getHost() != null && !c.getHost().isBlank()
                && c.getDatabaseName() != null && !c.getDatabaseName().isBlank()
                && c.getUsername() != null && !c.getUsername().isBlank()
                && c.getPasswordEncriptada() != null && !c.getPasswordEncriptada().isBlank()
                && c.getPuerto() != null;
    }

    private static void closeQuietly(ManagedPool pool) {
        if (pool == null || pool.ds == null || pool.ds.isClosed()) {
            return;
        }
        try {
            pool.ds.close();
        } catch (Exception ex) {
            log.warn("No se pudo cerrar el pool {}: {}", pool.ds.getPoolName(), ex.getMessage());
        }
    }

    private record ManagedPool(HikariDataSource ds, String fingerprint) {
        boolean isOpen() {
            return ds != null && !ds.isClosed();
        }
    }
}
