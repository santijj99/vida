package com.vida.apirest.servicies.afip;

import com.vida.apirest.config.AfipProperties;
import com.vida.apirest.model.almacen.Sucursal;
import com.vida.apirest.model.empresa.Empresa;
import com.vida.apirest.model.empresa.EmpresaAfipConfig;
import com.vida.apirest.model.venta.Venta;
import com.vida.apirest.repositories.EmpresaAfipConfigRepository;
import com.vida.apirest.repositories.SucursalRepository;
import com.vida.apirest.repositories.UsuarioSucursalRepository;
import com.vida.apirest.security.AppUserDetails;
import com.vida.apirest.utils.AfipCertificateLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AfipContextService {

    private final AfipProperties afipProperties;
    private final EmpresaAfipConfigRepository empresaAfipConfigRepository;
    private final SucursalRepository sucursalRepository;
    private final UsuarioSucursalRepository usuarioSucursalRepository;

    @Transactional(readOnly = true)
    public AfipContext resolveForVenta(Venta venta) {
        return resolveOptionalForVenta(venta)
                .orElseThrow(() -> new IllegalStateException(
                        "La empresa no tiene ARCA habilitado. Configurá el módulo ARCA o cobrá sin tarjeta/QR."));
    }

    /**
     * Igual que {@link #resolveForVenta} pero sin lanzar si la empresa no tiene ARCA.
     * Usar en cobro automático para no marcar rollback-only la venta.
     */
    @Transactional(readOnly = true)
    public Optional<AfipContext> resolveOptionalForVenta(Venta venta) {
        if (venta == null || venta.getSucursal() == null) {
            return Optional.empty();
        }
        Sucursal sucursal = venta.getSucursal();
        if (sucursal.getEmpresa() == null) {
            sucursal = sucursalRepository.findById(sucursal.getId()).orElse(null);
            if (sucursal == null || sucursal.getEmpresa() == null) {
                return Optional.empty();
            }
        }
        return resolveOptionalForEmpresaId(sucursal.getEmpresa().getId());
    }

    @Transactional(readOnly = true)
    public AfipContext resolveForEmpresaId(Long empresaId) {
        EmpresaAfipConfig config = empresaAfipConfigRepository.findByEmpresaIdWithEmpresa(empresaId)
                .orElseThrow(() -> new IllegalStateException(
                        "La empresa no tiene configuración AFIP. Configurá ARCA en el módulo de empresas."));
        return buildContext(config);
    }

    @Transactional(readOnly = true)
    public Optional<AfipContext> resolveOptionalForEmpresaId(Long empresaId) {
        return empresaAfipConfigRepository.findByEmpresaIdWithEmpresa(empresaId)
                .filter(EmpresaAfipConfig::isAfipHabilitado)
                .flatMap(config -> {
                    try {
                        return Optional.of(buildContext(config));
                    } catch (IllegalStateException e) {
                        return Optional.empty();
                    }
                });
    }

    @Transactional(readOnly = true)
    public AfipContext resolveForCurrentUser() {
        Long empresaId = resolveEmpresaIdForCurrentUser()
                .orElseThrow(() -> new IllegalStateException(
                        "No se pudo determinar la empresa del usuario para operar con ARCA"));
        return resolveForEmpresaId(empresaId);
    }

    @Transactional(readOnly = true)
    public Optional<Long> resolveEmpresaIdForCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof AppUserDetails details)) {
            return Optional.empty();
        }

        Long usuarioId = details.getUsuario().getId();

        List<Long> sucursalIds = usuarioSucursalRepository.findSucursalIdsByUsuarioId(usuarioId);
        if (sucursalIds.isEmpty()) {
            return Optional.empty();
        }

        return sucursalRepository.findById(sucursalIds.get(0))
                .map(s -> s.getEmpresa().getId());
    }

    @Transactional(readOnly = true)
    public List<AfipContext> resolveAllHabilitadas() {
        return empresaAfipConfigRepository.findAllHabilitadasWithEmpresa().stream()
                .map(this::buildContext)
                .toList();
    }

    public void runWithContext(AfipContext context, Runnable action) {
        AfipContextHolder.set(context);
        try {
            action.run();
        } finally {
            AfipContextHolder.clear();
        }
    }

    public <T> T callWithContext(AfipContext context, AfipCallable<T> action) throws Exception {
        AfipContextHolder.set(context);
        try {
            return action.call();
        } finally {
            AfipContextHolder.clear();
        }
    }

    private AfipContext buildContext(EmpresaAfipConfig config) {
        Empresa empresa = config.getEmpresa();
        if (!config.isAfipHabilitado()) {
            throw new IllegalStateException("ARCA no está habilitado para la empresa " + empresa.getNombre());
        }
        if (empresa.getCuit() == null || empresa.getCuit().isBlank()) {
            throw new IllegalStateException("La empresa " + empresa.getNombre() + " no tiene CUIT configurado");
        }

        Path certDir = resolveCertificadosDir(config);
        String direccion = formatearDireccionEmpresa(empresa);

        return new AfipContext(
                empresa.getId(),
                empresa.getCuit(),
                empresa.getRazonSocial() != null && !empresa.getRazonSocial().isBlank()
                        ? empresa.getRazonSocial()
                        : empresa.getNombre(),
                direccion,
                config.getCondicionIva(),
                config.getIibb(),
                config.getInicioActividad(),
                config.getPtoVta() != null ? config.getPtoVta() : 1,
                config.getCbteTipoDefault() != null ? config.getCbteTipoDefault() : 6,
                config.isHomologacion(),
                certDir,
                config.getClavePrivadaPassword()
        );
    }

    public Path resolveCertificadosDir(EmpresaAfipConfig config) {
        String custom = config.getCertificadosDirectorio();
        if (custom != null && !custom.isBlank() && esRutaCertificadosUsable(custom.trim())) {
            return Paths.get(custom.trim()).toAbsolutePath().normalize();
        }
        return defaultCertificadosDir(config.getEmpresa().getId());
    }

    /** Carpeta canónica en el volumen del servidor: {base}/{empresaId}/ */
    public Path defaultCertificadosDir(Long empresaId) {
        return Paths.get(afipProperties.getCertificadosBaseDir(), String.valueOf(empresaId))
                .toAbsolutePath()
                .normalize();
    }

    /**
     * Detecta rutas de otro SO (p. ej. C:/Users/... guardadas desde Windows
     * y resueltas en Linux Docker como /app/C:/Users/...).
     */
    public boolean esRutaCertificadosUsable(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String path = raw.trim();
        boolean windowsOs = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win");
        // Letra de unidad Windows fuera de Windows, o path ya "mezclado" /app/C:/...
        if (path.matches("(?i)^[a-z]:[\\\\/].*") || path.matches("(?i).*[/\\\\][a-z]:[/\\\\].*")) {
            return windowsOs && path.matches("(?i)^[a-z]:[\\\\/].*");
        }
        try {
            Paths.get(path);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void validarCertificados(AfipContext context) {
        try {
            AfipCertificateLoader.AfipCredentials credentials = AfipCertificateLoader.resolve(
                    context.certificadosDir(), context.clavePrivadaPassword());
            AfipCertificateLoader.validarCertificadoParaAmbiente(
                    credentials.certificate(), context.homologacion());
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "No se pudieron leer los certificados AFIP en " + context.certificadosDir()
                            + ". Se requieren certificado.crt + MiClavePrivada.key o un archivo .p12. "
                            + e.getMessage(), e);
        }
    }

    private String formatearDireccionEmpresa(Empresa empresa) {
        StringBuilder sb = new StringBuilder();
        if (empresa.getDomicilio() != null && !empresa.getDomicilio().isBlank()) {
            sb.append(empresa.getDomicilio().trim());
        }
        if (empresa.getCiudad() != null && !empresa.getCiudad().isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(empresa.getCiudad().trim());
        }
        if (empresa.getProvincia() != null && !empresa.getProvincia().isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(empresa.getProvincia().trim());
        }
        return sb.toString();
    }

    @FunctionalInterface
    public interface AfipCallable<T> {
        T call() throws Exception;
    }
}
