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

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SistemaLicenciaService {

    private final LicenciaProperties properties;
    private final LicenciaServerClient client;
    private final LicenciaEstadoCacheRepository cacheRepository;
    private final DeviceUuidResolver deviceUuidResolver;

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
                    .codigoLicencia(maskCodigo(resolveCodigoLicencia()))
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

        return toResponse(cache, resolveCodigoLicencia());
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
        // Mismo código que en login: header/JWT (multi-tenant) o app.licencia.codigo.
        String codigo = resolveCodigoLicencia();
        String deviceUuid = resolveDeviceUuid();
        cache.setDeviceUuid(deviceUuid);

        if (codigo.isBlank()) {
            cache.setValida(false);
            cache.setEstado("SIN_CONFIGURAR");
            cache.setCodigoError("LICENCIA_SIN_CODIGO");
            cache.setMensaje(properties.isMultiTenant()
                    ? "Falta el código de licencia del request (X-Licencia-Codigo / login)"
                    : "Falta configurar app.licencia.codigo");
            cache.setServidorInalcanzable(false);
            cache.setModoGracia(false);
            cache.setUltimaValidacion(Instant.now());
            cacheRepository.save(cache);
            return;
        }

        ValidacionRemotaResult result = client.validar(
                codigo, deviceUuid, deviceUuidResolver.resolveNombre());
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

    /**
     * Prioridad: código del tenant del request (igual que al ingresar) → config local.
     */
    private String resolveCodigoLicencia() {
        String fromTenant = com.vida.apirest.tenant.TenantContext.getCodigoLicencia();
        if (fromTenant != null && !fromTenant.isBlank()) {
            return fromTenant.trim();
        }
        String configured = properties.getCodigo();
        return configured == null ? "" : configured.trim();
    }

    private static String maskCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return null;
        }
        String c = codigo.trim();
        if (c.length() <= 4) {
            return c;
        }
        return "••••" + c.substring(c.length() - 4);
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
            cache.setMensaje("Servidor de licencias inalcanzable; operando en período de gracia ("
                    + properties.getGraciaDias() + " días desde el último OK)");
        } else {
            cache.setModoGracia(false);
            cache.setValida(false);
            cache.setEstado("DESCONOCIDO");
            cache.setCodigoError("GRACIA_VENCIDA");
            cache.setMensaje("Período de gracia de " + properties.getGraciaDias()
                    + " días vencido y el servidor de licencias sigue inalcanzable");
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

    private SistemaInfoResponse toResponse(LicenciaEstadoCache cache, String codigoLicencia) {
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
                .codigoLicencia(maskCodigo(codigoLicencia))
                .deviceUuid(resolveDeviceUuid())
                .ultimaValidacion(cache.getUltimaValidacion())
                .ultimoExito(cache.getUltimoExito())
                .build();
    }

    private String resolveDeviceUuid() {
        return deviceUuidResolver.resolve();
    }

    private String resolveVersion() {
        Package pkg = getClass().getPackage();
        if (pkg != null && pkg.getImplementationVersion() != null) {
            return pkg.getImplementationVersion();
        }
        return "0.0.1-SNAPSHOT";
    }
}
