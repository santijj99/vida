package com.vida.apirest.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

/**
 * Persiste la última validación OK de cada licencia (conexión DB + timestamp)
 * para poder iniciar sesión sin internet durante el período de gracia.
 */
@Slf4j
@Component
public class TenantConnectionCacheStore {

    private static final Path CACHE_DIR = Path.of("data", "tenant-cache");

    private final ObjectMapper mapper = new ObjectMapper();

    public Optional<CachedTenantConnection> find(String codigoLicencia) {
        Path file = fileFor(codigoLicencia);
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try {
            CachedTenantConnection cached = mapper.readValue(file.toFile(), CachedTenantConnection.class);
            return Optional.ofNullable(cached);
        } catch (Exception ex) {
            log.warn("No se pudo leer caché de tenant {}: {}", codigoLicencia, ex.getMessage());
            return Optional.empty();
        }
    }

    public void save(CachedTenantConnection cached) {
        if (cached == null || cached.getCodigoLicencia() == null || cached.getCodigoLicencia().isBlank()) {
            return;
        }
        try {
            Files.createDirectories(CACHE_DIR);
            Path file = fileFor(cached.getCodigoLicencia());
            mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), cached);
        } catch (Exception ex) {
            log.warn("No se pudo guardar caché de tenant {}: {}", cached.getCodigoLicencia(), ex.getMessage());
        }
    }

    private Path fileFor(String codigo) {
        // Evita path traversal / caracteres raros en el nombre de archivo.
        String safe = codigo.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
        if (safe.length() > 120) {
            safe = safe.substring(0, 120);
        }
        return CACHE_DIR.resolve(safe + ".json");
    }

    @Data
    public static class CachedTenantConnection {
        private String codigoLicencia;
        private String deviceUuid;
        /** Epoch millis del último OK remoto. */
        private Long ultimoExitoEpochMs;
        private String host;
        private Integer puerto;
        private String databaseName;
        private String username;
        private String passwordEncriptada;
        private Boolean ssl;
        private String empresaNombre;
        private String planNombre;

        public Instant ultimoExito() {
            return ultimoExitoEpochMs == null ? null : Instant.ofEpochMilli(ultimoExitoEpochMs);
        }

        public void setUltimoExito(Instant instant) {
            this.ultimoExitoEpochMs = instant == null ? null : instant.toEpochMilli();
        }
    }
}
