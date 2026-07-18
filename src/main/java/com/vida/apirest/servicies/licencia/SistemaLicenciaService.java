package com.vida.apirest.servicies.licencia;

import com.vida.apirest.config.LicenciaProperties;
import com.vida.apirest.dto.sistema.SistemaInfoResponse;
import com.vida.apirest.model.sistema.LicenciaEstadoCache;
import com.vida.apirest.repository.LicenciaEstadoCacheRepository;
import com.vida.apirest.servicies.licencia.LicenciaServerClient.ValidacionRemotaResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SistemaLicenciaService {

    private static final Path DEVICE_UUID_PATH = Path.of("data", "device-uuid.txt");

    private final LicenciaProperties properties;
    private final LicenciaServerClient client;
    private final LicenciaEstadoCacheRepository cacheRepository;

    @Transactional
    public SistemaInfoResponse obtenerInfo(boolean forzarRefresh) {
        if (!properties.isEnabled()) {
            return SistemaInfoResponse.builder()
                    .aplicacion("ATHLAND")
                    .version(resolveVersion())
                    .licenciaHabilitada(false)
                    .licenciaValida(true)
                    .modoGracia(false)
                    .servidorInalcanzable(false)
                    .estado("DESHABILITADA")
                    .mensaje("El control de licencias está deshabilitado en este servidor")
                    .deviceUuid(resolveDeviceUuid())
                    .build();
        }

        LicenciaEstadoCache cache = getOrCreateCache();
        boolean necesitaRefresh = forzarRefresh
                || cache.getUltimaValidacion() == null
                || cache.getUltimaValidacion().isBefore(
                Instant.now().minus(Math.max(1, properties.getCacheMinutos()), ChronoUnit.MINUTES));

        if (necesitaRefresh) {
            refrescarContraServidor(cache);
        } else {
            aplicarGraciaSiCorresponde(cache);
        }

        return toResponse(cache);
    }

    @Transactional
    public boolean isLicenciaOperativa() {
        if (!properties.isEnabled()) {
            return true;
        }
        SistemaInfoResponse info = obtenerInfo(false);
        return info.isLicenciaValida();
    }

    private void refrescarContraServidor(LicenciaEstadoCache cache) {
        String codigo = properties.getCodigo() == null ? "" : properties.getCodigo().trim();
        String deviceUuid = resolveDeviceUuid();
        cache.setDeviceUuid(deviceUuid);

        if (codigo.isBlank()) {
            cache.setValida(false);
            cache.setEstado("SIN_CONFIGURAR");
            cache.setCodigoError("LICENCIA_SIN_CODIGO");
            cache.setMensaje("Falta configurar app.licencia.codigo");
            cache.setServidorInalcanzable(false);
            cache.setModoGracia(false);
            cache.setUltimaValidacion(Instant.now());
            cacheRepository.save(cache);
            return;
        }

        ValidacionRemotaResult result = client.validar(codigo, deviceUuid);
        Instant now = Instant.now();
        cache.setUltimaValidacion(now);
        cache.setServidorInalcanzable(!result.isAlcanzable());

        if (!result.isAlcanzable()) {
            aplicarGraciaSiCorresponde(cache);
            if (!cache.isModoGracia()) {
                cache.setValida(false);
                cache.setEstado("DESCONOCIDO");
                cache.setCodigoError(result.getCodigoError());
                cache.setMensaje(result.getMensaje());
            }
            cacheRepository.save(cache);
            return;
        }

        cache.setModoGracia(false);
        cache.setValida(result.isValida());
        cache.setEstado(result.getEstado());
        cache.setCodigoError(result.getCodigoError());
        cache.setMensaje(result.getMensaje());

        if (result.isValida()) {
            cache.setUltimoExito(now);
            cache.setEmpresaNombre(result.getEmpresaNombre());
            cache.setPlanNombre(result.getPlanNombre());
            cache.setFechaVencimiento(result.getFechaVencimiento());
            cache.setCantidadMaximaDispositivos(result.getCantidadMaximaDispositivos());
            cache.setCantidadMaximaSucursales(result.getCantidadMaximaSucursales());
        }

        cacheRepository.save(cache);
    }

    private void aplicarGraciaSiCorresponde(LicenciaEstadoCache cache) {
        if (!cache.isServidorInalcanzable() || cache.getUltimoExito() == null) {
            cache.setModoGracia(false);
            return;
        }
        Instant limite = cache.getUltimoExito()
                .plus(Math.max(0, properties.getGraciaDias()), ChronoUnit.DAYS);
        if (Instant.now().isBefore(limite)) {
            cache.setModoGracia(true);
            cache.setValida(true);
            cache.setEstado("GRACIA");
            cache.setCodigoError(null);
            cache.setMensaje("Servidor de licencias inalcanzable; operando en período de gracia");
        } else {
            cache.setModoGracia(false);
            cache.setValida(false);
            cache.setEstado("DESCONOCIDO");
            cache.setCodigoError("GRACIA_VENCIDA");
            cache.setMensaje("Período de gracia vencido y el servidor de licencias sigue inalcanzable");
        }
    }

    private LicenciaEstadoCache getOrCreateCache() {
        return cacheRepository.findById(1L).orElseGet(() -> {
            LicenciaEstadoCache created = new LicenciaEstadoCache();
            created.setId(1L);
            created.setDeviceUuid(resolveDeviceUuid());
            return cacheRepository.save(created);
        });
    }

    private SistemaInfoResponse toResponse(LicenciaEstadoCache cache) {
        return SistemaInfoResponse.builder()
                .aplicacion("ATHLAND")
                .version(resolveVersion())
                .licenciaHabilitada(true)
                .licenciaValida(cache.isValida())
                .modoGracia(cache.isModoGracia())
                .servidorInalcanzable(cache.isServidorInalcanzable())
                .estado(cache.getEstado())
                .codigoError(cache.getCodigoError())
                .mensaje(cache.getMensaje())
                .empresaNombre(cache.getEmpresaNombre())
                .planNombre(cache.getPlanNombre())
                .fechaVencimiento(cache.getFechaVencimiento())
                .cantidadMaximaDispositivos(cache.getCantidadMaximaDispositivos())
                .cantidadMaximaSucursales(cache.getCantidadMaximaSucursales())
                .deviceUuid(cache.getDeviceUuid() != null ? cache.getDeviceUuid() : resolveDeviceUuid())
                .ultimaValidacion(cache.getUltimaValidacion())
                .ultimoExito(cache.getUltimoExito())
                .build();
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
        } catch (IOException e) {
            log.warn("No se pudo persistir device UUID, usando efímero: {}", e.getMessage());
            return UUID.randomUUID().toString();
        }
    }

    private String resolveVersion() {
        Package pkg = getClass().getPackage();
        if (pkg != null && pkg.getImplementationVersion() != null) {
            return pkg.getImplementationVersion();
        }
        return "0.0.1-SNAPSHOT";
    }
}
