package com.vida.apirest.servicies.afip;

import com.vida.apirest.model.empresa.EmpresaAfipConfig;
import com.vida.apirest.repositories.EmpresaAfipConfigRepository;
import com.vida.apirest.utils.AFIPTokenLoader;
import com.vida.apirest.utils.AfipTaAlreadyAuthenticatedException;
import com.vida.apirest.utils.AfipWsaaClient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Guarda y recupera TA.xml generado por Java (archivo en disco + copia en base de datos).
 */
@Service
@RequiredArgsConstructor
public class AfipTaStorageService {

    private static final Logger log = LoggerFactory.getLogger(AfipTaStorageService.class);
    private static final String SERVICE_WSFE = "wsfe";
    private static final String SERVICE_PADRON_A13 = "ws_sr_padron_a13";
    private static final int REINTENTOS_HOMOLOGACION = 6;
    private static final long ESPERA_REINTENTO_MS = 10_000L;

    private final EmpresaAfipConfigRepository empresaAfipConfigRepository;

    public Path resolverRutaTa(AfipContext context, String service) {
        Path certDir = context.certificadosDir();

        Path taService = certDir.resolve("TA-" + service + ".xml");
        if (Files.exists(taService)) {
            return taService;
        }

        if (SERVICE_PADRON_A13.equals(service)) {
            Path legacy = certDir.resolve("TA-padron.xml");
            if (Files.exists(legacy)) {
                return legacy;
            }
        }

        if (SERVICE_WSFE.equals(service)) {
            Path taXml = certDir.resolve("TA.xml");
            if (Files.exists(taXml)) {
                return taXml;
            }
        }

        return null;
    }

    @Transactional(readOnly = true)
    public Optional<AFIPTokenLoader.TokenSign> cargarToken(AfipContext context, String service) throws Exception {
        Path taPath = resolverRutaTa(context, service);
        if (taPath != null) {
            try {
                AFIPTokenLoader.TokenSign tokenSign =
                        AFIPTokenLoader.loadFromXml(taPath.toAbsolutePath().toString());
                if (!AFIPTokenLoader.tokenEsParaServicio(tokenSign, service)) {
                    log.warn("TA en {} no corresponde al servicio {} (empresa {}). Se ignorará.",
                            taPath, service, context.empresaId());
                } else {
                    return Optional.of(tokenSign);
                }
            } catch (Exception e) {
                log.warn("TA.xml inválido en {} (empresa {}): {}", taPath, context.empresaId(), e.getMessage());
                eliminarTaInvalidoEnDisco(context.certificadosDir(), service);
            }
        }
        return cargarDesdeBaseDeDatos(context.empresaId(), service);
    }

    @Transactional
    public void guardarTa(AfipContext context, String service, String taXml) throws Exception {
        if (!AFIPTokenLoader.looksLikeTaXml(taXml)) {
            throw new IllegalStateException("AFIP devolvió un TA.xml inválido (no es XML)");
        }
        String taNormalizado = AFIPTokenLoader.sanitizeXml(taXml);
        Path certDir = context.certificadosDir();
        Files.createDirectories(certDir);

        Path taService = certDir.resolve("TA-" + service + ".xml");
        Files.writeString(taService, taNormalizado, StandardCharsets.UTF_8);
        if (SERVICE_WSFE.equals(service)) {
            Files.writeString(certDir.resolve("TA.xml"), taNormalizado, StandardCharsets.UTF_8);
        }

        if (SERVICE_WSFE.equals(service)) {
            empresaAfipConfigRepository.findByEmpresaId(context.empresaId()).ifPresent(config -> {
                config.setTaXmlWsfe(taNormalizado);
                empresaAfipConfigRepository.save(config);
            });
        }

        log.info("TA.xml guardado en {} (empresa {}, servicio {})", certDir, context.empresaId(), service);
    }

    private void eliminarTaInvalidoEnDisco(Path certDir, String service) {
        List<Path> candidatos = new ArrayList<>();
        candidatos.add(certDir.resolve("TA-" + service + ".xml"));
        if (SERVICE_PADRON_A13.equals(service)) {
            candidatos.add(certDir.resolve("TA-padron.xml"));
        }
        if (SERVICE_WSFE.equals(service)) {
            candidatos.add(certDir.resolve("TA.xml"));
        }
        for (Path candidato : candidatos) {
            try {
                if (Files.deleteIfExists(candidato)) {
                    log.warn("Se eliminó TA.xml inválido: {}", candidato);
                }
            } catch (Exception e) {
                log.warn("No se pudo eliminar TA inválido {}: {}", candidato, e.getMessage());
            }
        }
    }

    /**
     * Restaura TA.xml en disco desde la copia en base de datos.
     */
    @Transactional
    public boolean restaurarTaEnDisco(AfipContext context, String service) {
        try {
            limpiarTaCorruptoEnBaseDeDatos(context.empresaId(), service);
            Optional<String> taXml = leerXmlDesdeBaseDeDatos(context.empresaId(), service);
            if (taXml.isEmpty()) {
                return false;
            }
            guardarTa(context, service, taXml.get());
            return true;
        } catch (Exception e) {
            log.warn("No se pudo restaurar TA.xml desde base de datos (empresa {}): {}",
                    context.empresaId(), e.getMessage());
            return false;
        }
    }

    private Optional<AFIPTokenLoader.TokenSign> cargarDesdeBaseDeDatos(Long empresaId, String service) throws Exception {
        Optional<String> taXml = leerXmlDesdeBaseDeDatos(empresaId, service);
        if (taXml.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(AFIPTokenLoader.loadFromXmlContent(taXml.get()));
    }

    private Optional<String> leerXmlDesdeBaseDeDatos(Long empresaId, String service) {
        if (!SERVICE_WSFE.equals(service)) {
            return Optional.empty();
        }
        return empresaAfipConfigRepository.findByEmpresaId(empresaId)
                .map(EmpresaAfipConfig::getTaXmlWsfe)
                .filter(xml -> xml != null && !xml.isBlank())
                .filter(AFIPTokenLoader::looksLikeTaXml);
    }

    @Transactional
    public void limpiarTaCorruptoEnBaseDeDatos(Long empresaId, String service) {
        if (!SERVICE_WSFE.equals(service)) {
            return;
        }
        empresaAfipConfigRepository.findByEmpresaId(empresaId).ifPresent(config -> {
            String xml = config.getTaXmlWsfe();
            if (xml != null && !xml.isBlank() && !AFIPTokenLoader.looksLikeTaXml(xml)) {
                log.warn("Copia de TA.xml corrupta en base de datos (empresa {}). Se descarta.", empresaId);
                config.setTaXmlWsfe(null);
                empresaAfipConfigRepository.save(config);
            }
        });
    }

    /**
     * Pide TA a AFIP por Java (como wsaa-client.php) y lo guarda en disco + base de datos.
     * En homologación reintenta varias veces si AFIP aún no liberó el cupo.
     */
    @Transactional
    public AFIPTokenLoader.TokenSign generarYGuardar(AfipContext context, String service) throws Exception {
        return generarYGuardar(context, service, context.homologacion());
    }

    @Transactional
    public AFIPTokenLoader.TokenSign generarYGuardar(AfipContext context, String service, boolean homologacion)
            throws Exception {
        int intentos = homologacion ? REINTENTOS_HOMOLOGACION : 1;
        AfipTaAlreadyAuthenticatedException ultimoBloqueo = null;

        for (int intento = 1; intento <= intentos; intento++) {
            try {
                String taXml = AfipWsaaClient.solicitarTaXml(context, service);
                guardarTa(context, service, taXml);
                return AFIPTokenLoader.loadFromXmlContent(taXml);
            } catch (AfipTaAlreadyAuthenticatedException e) {
                ultimoBloqueo = e;
                if (resolverCopiaLocalDisponible(context, service)) {
                    return cargarToken(context, service)
                            .orElseThrow(() -> new IllegalStateException("No se pudo leer el TA local."));
                }
                if (homologacion && intento < intentos) {
                    log.info("Homologación: AFIP aún tiene TA vigente (intento {}/{}). Reintentando en {}s...",
                            intento, intentos, ESPERA_REINTENTO_MS / 1000);
                    Thread.sleep(ESPERA_REINTENTO_MS);
                }
            }
        }

        if (ultimoBloqueo != null) {
            throw ultimoBloqueo;
        }
        throw new IllegalStateException("No se pudo generar el TA con Java WSAA.");
    }

    /**
     * Si falta TA.xml, intenta copiarlo desde ubicaciones de desarrollo (homologación).
     */
    @Transactional
    public boolean importarTaAlternativoSiFalta(AfipContext context, String service) {
        if (!context.homologacion() || resolverRutaTa(context, service) != null) {
            return false;
        }
        for (Path origen : ubicacionesBootstrapHomologacion(context.certificadosDir(), service)) {
            if (!Files.isRegularFile(origen)) {
                continue;
            }
            try {
                String taXml = Files.readString(origen, StandardCharsets.UTF_8);
                if (!taXml.contains("<loginTicketResponse")
                        || !AFIPTokenLoader.tokenEsParaServicio(taXml, service)) {
                    continue;
                }
                guardarTa(context, service, taXml);
                log.info("TA.xml importado desde {} hacia {}", origen, context.certificadosDir());
                return true;
            } catch (Exception e) {
                log.warn("No se pudo importar TA desde {}: {}", origen, e.getMessage());
            }
        }
        return importarTaDesdeClasspath(context, service);
    }

    private boolean importarTaDesdeClasspath(AfipContext context, String service) {
        try {
            ClassPathResource resource = new ClassPathResource("certificados/TA-homologacion.xml");
            if (!resource.exists()) {
                return false;
            }
            try (InputStream is = resource.getInputStream()) {
                String taXml = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                guardarTa(context, service, taXml);
                log.info("TA.xml de homologación importado desde classpath hacia {}", context.certificadosDir());
                return true;
            }
        } catch (Exception e) {
            log.warn("No se pudo importar TA desde classpath: {}", e.getMessage());
            return false;
        }
    }

    private boolean resolverCopiaLocalDisponible(AfipContext context, String service) {
        if (restaurarTaEnDisco(context, service)) {
            return true;
        }
        return importarTaAlternativoSiFalta(context, service);
    }

    private List<Path> ubicacionesBootstrapHomologacion(Path certDir, String service) {
        List<Path> candidatos = new ArrayList<>();
        String env = System.getenv("AFIP_TA_BOOTSTRAP");
        if (env != null && !env.isBlank()) {
            candidatos.add(Path.of(env.trim()));
        }

        candidatos.add(certDir.resolve("TA-" + service + ".xml"));
        if (SERVICE_PADRON_A13.equals(service)) {
            candidatos.add(certDir.resolve("TA-padron.xml"));
        }
        if (SERVICE_WSFE.equals(service)) {
            candidatos.add(certDir.resolve("TA.xml"));
            candidatos.add(certDir.resolve("TA-wsfe.xml"));
        }
        try (Stream<Path> enCarpeta = Files.list(certDir)) {
            enCarpeta.filter(p -> p.getFileName().toString().startsWith("TA")
                            && p.getFileName().toString().endsWith(".xml"))
                    .forEach(candidatos::add);
        } catch (Exception ignored) {
            // sin permisos de listado
        }

        Path parent = certDir.getParent();
        if (parent != null) {
            if (SERVICE_WSFE.equals(service)) {
                candidatos.add(parent.resolve("testing").resolve("TA.xml"));
                candidatos.add(parent.resolve("TA.xml"));
            }
            try (Stream<Path> stream = Files.list(parent)) {
                stream.filter(p -> p.getFileName().toString().startsWith("TA")
                                && p.getFileName().toString().endsWith(".xml"))
                        .forEach(candidatos::add);
            } catch (Exception ignored) {
                // sin permisos de listado
            }
        }

        return candidatos.stream().distinct().toList();
    }
}
