package com.vida.apirest.servicies.licencia;

import com.vida.apirest.config.LicenciaProperties;
import com.vida.apirest.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Resuelve el UUID de dispositivo que se reporta al servidor de licencias.
 * <p>
 * Prioridad: UUID del equipo cliente (header {@code X-Device-Uuid}) → UUID
 * configurado → UUID persistido del servidor. El header es lo importante: con
 * un solo apirest compartido, el UUID del servidor haría que todos los equipos
 * cuenten como un único dispositivo.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceUuidResolver {

    private static final Path DEVICE_UUID_PATH = Path.of("data", "device-uuid.txt");

    private final LicenciaProperties properties;

    public String resolve() {
        String fromRequest = TenantContext.getDeviceUuid();
        if (fromRequest != null && !fromRequest.isBlank()) {
            return normalizar(fromRequest);
        }
        return resolveServidor();
    }

    /** UUID de esta instalación de apirest, sin mirar el request. */
    public String resolveServidor() {
        String configured = properties.getDeviceUuid();
        if (configured != null && !configured.isBlank()) {
            return normalizar(configured);
        }
        try {
            if (Files.exists(DEVICE_UUID_PATH)) {
                String existing = Files.readString(DEVICE_UUID_PATH).trim();
                if (!existing.isBlank()) {
                    return normalizar(existing);
                }
            }
            Files.createDirectories(DEVICE_UUID_PATH.getParent());
            String generated = UUID.randomUUID().toString();
            Files.writeString(DEVICE_UUID_PATH, generated);
            return generated;
        } catch (Exception e) {
            log.warn("No se pudo persistir device UUID, usando efímero: {}", e.getMessage());
            return UUID.randomUUID().toString();
        }
    }

    /** Nombre legible del equipo cliente, si lo envió (header {@code X-Device-Nombre}). */
    public String resolveNombre() {
        String nombre = TenantContext.getDeviceNombre();
        if (nombre == null || nombre.isBlank()) {
            return null;
        }
        String v = nombre.trim();
        return v.length() > 150 ? v.substring(0, 150) : v;
    }

    /** La columna uuid en licencias es varchar(64). */
    private static String normalizar(String raw) {
        String v = raw.trim();
        return v.length() > 64 ? v.substring(0, 64) : v;
    }
}
