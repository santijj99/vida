package com.vida.apirest.servicies;

import com.vida.apirest.dto.empresa.EmpresaAfipConfigRequest;
import com.vida.apirest.dto.empresa.EmpresaAfipConfigResponse;
import com.vida.apirest.model.empresa.Empresa;
import com.vida.apirest.model.empresa.EmpresaAfipConfig;
import com.vida.apirest.repositories.EmpresaAfipConfigRepository;
import com.vida.apirest.repositories.EmpresaRepository;
import com.vida.apirest.servicies.afip.AfipContextService;
import com.vida.apirest.utils.AfipCertificateLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmpresaAfipConfigService {

    private final EmpresaRepository empresaRepository;
    private final EmpresaAfipConfigRepository empresaAfipConfigRepository;
    private final AfipContextService afipContextService;

    @Transactional
    public EmpresaAfipConfigResponse obtener(Long empresaId) {
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));
        EmpresaAfipConfig config = empresaAfipConfigRepository.findByEmpresaId(empresaId)
                .orElseGet(() -> configPorDefecto(empresa));
        if (sanearRutaCertificadosSiCorresponde(config) && config.getId() != null) {
            config = empresaAfipConfigRepository.save(config);
        }
        return toResponse(empresa, config);
    }

    @Transactional
    public EmpresaAfipConfigResponse guardar(Long empresaId, EmpresaAfipConfigRequest request) {
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));

        EmpresaAfipConfig config = empresaAfipConfigRepository.findByEmpresaId(empresaId)
                .orElseGet(() -> {
                    EmpresaAfipConfig nuevo = new EmpresaAfipConfig();
                    nuevo.setEmpresa(empresa);
                    return nuevo;
                });

        if (request.getAfipHabilitado() != null) {
            config.setAfipHabilitado(request.getAfipHabilitado());
        }
        if (request.getPtoVta() != null) {
            config.setPtoVta(request.getPtoVta());
        }
        if (request.getCbteTipoDefault() != null) {
            config.setCbteTipoDefault(request.getCbteTipoDefault());
        }
        if (request.getCondicionIva() != null) {
            config.setCondicionIva(request.getCondicionIva());
        }
        if (request.getIibb() != null) {
            config.setIibb(request.getIibb());
        }
        if (request.getInicioActividad() != null) {
            config.setInicioActividad(request.getInicioActividad());
        }
        if (request.getCertificadosDirectorio() != null) {
            String raw = request.getCertificadosDirectorio().trim();
            if (raw.isBlank() || !afipContextService.esRutaCertificadosUsable(raw)) {
                config.setCertificadosDirectorio(null);
            } else {
                config.setCertificadosDirectorio(raw);
            }
        }
        if (request.getClavePrivadaPassword() != null) {
            config.setClavePrivadaPassword(request.getClavePrivadaPassword().isBlank()
                    ? null
                    : request.getClavePrivadaPassword());
        }

        sanearRutaCertificadosSiCorresponde(config);
        EmpresaAfipConfig guardada = empresaAfipConfigRepository.save(config);
        return toResponse(empresa, guardada);
    }

    /**
     * Sube certificados al volumen del servidor ({base}/{empresaId}/) y limpia
     * cualquier ruta custom (p. ej. paths de Windows en un API Docker).
     * Acepta certificado.crt + MiClavePrivada.key, o un PKCS#12 (.p12).
     */
    @Transactional
    public EmpresaAfipConfigResponse subirCertificados(
            Long empresaId,
            MultipartFile certificado,
            MultipartFile clavePrivada,
            MultipartFile pkcs12,
            String clavePrivadaPassword
    ) {
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada"));

        boolean tieneP12 = pkcs12 != null && !pkcs12.isEmpty();
        boolean tieneCrt = certificado != null && !certificado.isEmpty();
        boolean tieneKey = clavePrivada != null && !clavePrivada.isEmpty();

        if (tieneP12) {
            validarExtension(pkcs12, ".p12", ".pfx");
        } else if (tieneCrt && tieneKey) {
            validarExtension(certificado, ".crt", ".pem", ".cer");
            validarExtension(clavePrivada, ".key", ".pem");
        } else {
            throw new IllegalArgumentException(
                    "Subí certificado.crt + MiClavePrivada.key, o un archivo .p12");
        }

        EmpresaAfipConfig config = empresaAfipConfigRepository.findByEmpresaId(empresaId)
                .orElseGet(() -> {
                    EmpresaAfipConfig nuevo = new EmpresaAfipConfig();
                    nuevo.setEmpresa(empresa);
                    return nuevo;
                });

        Path certDir = afipContextService.defaultCertificadosDir(empresaId);
        try {
            Files.createDirectories(certDir);
            if (tieneP12) {
                Path destino = certDir.resolve("certificado.p12");
                try (var in = pkcs12.getInputStream()) {
                    Files.copy(in, destino, StandardCopyOption.REPLACE_EXISTING);
                }
            } else {
                Path destCrt = certDir.resolve("certificado.crt");
                Path destKey = certDir.resolve("MiClavePrivada.key");
                try (var in = certificado.getInputStream()) {
                    Files.copy(in, destCrt, StandardCopyOption.REPLACE_EXISTING);
                }
                try (var in = clavePrivada.getInputStream()) {
                    Files.copy(in, destKey, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se pudieron guardar los certificados en el servidor: " + e.getMessage(), e);
        }

        // Siempre usar la carpeta canónica del volumen (ignora paths locales del cliente).
        config.setCertificadosDirectorio(null);
        if (clavePrivadaPassword != null && !clavePrivadaPassword.isBlank()) {
            config.setClavePrivadaPassword(clavePrivadaPassword);
        }

        try {
            AfipCertificateLoader.resolve(certDir, config.getClavePrivadaPassword());
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Los archivos se guardaron pero no se pudieron leer como certificados AFIP: "
                            + e.getMessage(), e);
        }

        EmpresaAfipConfig guardada = empresaAfipConfigRepository.save(config);
        return toResponse(empresa, guardada);
    }

    private void validarExtension(MultipartFile file, String... allowed) {
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El archivo no tiene nombre");
        }
        String lower = name.toLowerCase(Locale.ROOT);
        for (String ext : allowed) {
            if (lower.endsWith(ext)) {
                return;
            }
        }
        throw new IllegalArgumentException(
                "Extensión no permitida en '" + name + "'. Esperado: " + String.join(", ", allowed));
    }

    /** @return true si se limpió una ruta inválida en memoria. */
    private boolean sanearRutaCertificadosSiCorresponde(EmpresaAfipConfig config) {
        String custom = config.getCertificadosDirectorio();
        if (custom != null && !custom.isBlank()
                && !afipContextService.esRutaCertificadosUsable(custom.trim())) {
            config.setCertificadosDirectorio(null);
            return true;
        }
        return false;
    }

    private EmpresaAfipConfig configPorDefecto(Empresa empresa) {
        EmpresaAfipConfig config = new EmpresaAfipConfig();
        config.setEmpresa(empresa);
        config.setAfipHabilitado(false);
        config.setPtoVta(1);
        config.setCbteTipoDefault(6);
        config.setCondicionIva("IVA Responsable Inscripto");
        return config;
    }

    private EmpresaAfipConfigResponse toResponse(Empresa empresa, EmpresaAfipConfig config) {
        Path certDir = afipContextService.resolveCertificadosDir(config);
        boolean certsOk = certificadosPresentesEn(certDir);

        return EmpresaAfipConfigResponse.builder()
                .empresaId(empresa.getId())
                .empresaNombre(empresa.getNombre())
                .cuit(empresa.getCuit())
                .razonSocial(empresa.getRazonSocial())
                .domicilio(empresa.getDomicilio())
                .afipHabilitado(config.isAfipHabilitado())
                .ptoVta(config.getPtoVta())
                .cbteTipoDefault(config.getCbteTipoDefault())
                .condicionIva(config.getCondicionIva())
                .iibb(config.getIibb())
                .inicioActividad(config.getInicioActividad())
                .certificadosDirectorio(certDir.toString())
                .certificadosPresentes(certsOk)
                .build();
    }

    private boolean certificadosPresentesEn(Path certDir) {
        if (Files.isRegularFile(certDir.resolve("certificado.crt"))
                && Files.isRegularFile(certDir.resolve("MiClavePrivada.key"))) {
            return true;
        }
        if (Files.isRegularFile(certDir.resolve("certificado.p12"))) {
            return true;
        }
        try {
            if (!Files.isDirectory(certDir)) {
                return false;
            }
            try (var stream = Files.list(certDir)) {
                return stream
                        .filter(Files::isRegularFile)
                        .anyMatch(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".p12"));
            }
        } catch (IOException e) {
            return false;
        }
    }
}
