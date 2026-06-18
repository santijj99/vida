package com.vida.apirest.servicies.afip;

import com.vida.apirest.config.AfipProperties;
import com.vida.apirest.dto.afip.TokenValidationResponse;
import com.vida.apirest.utils.AFIPTokenLoader;
import com.vida.apirest.utils.AfipTokenPathResolver;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "afip.enabled", havingValue = "true")
public class AFIPTokenValidatorService {

    private static final Logger log = LoggerFactory.getLogger(AFIPTokenValidatorService.class);
    private static final String SERVICE_NAME = "wsfe";

    private final AfipProperties afipProperties;
    private final WSAAService wsaaService;

    public TokenValidationResponse consultarEstadoToken() {
        if (!afipProperties.isEnabled()) {
            return TokenValidationResponse.builder()
                    .activo(false)
                    .mensaje("Módulo AFIP deshabilitado en el servidor")
                    .regenerado(false)
                    .build();
        }

        try {
            File taXmlFile = resolverArchivoToken();
            if (taXmlFile == null || !taXmlFile.exists()) {
                return TokenValidationResponse.builder()
                        .activo(false)
                        .mensaje("No se encontró el archivo TA.xml")
                        .regenerado(false)
                        .build();
            }

            AFIPTokenLoader.TokenSign tokenSign = AFIPTokenLoader.loadFromXml(taXmlFile.getAbsolutePath());
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date expiration = tokenSign.getExpiration();

            if (tokenSign.isValid()) {
                return TokenValidationResponse.builder()
                        .activo(true)
                        .mensaje("Token activo. Expira el " + (expiration != null ? sdf.format(expiration) : "desconocido"))
                        .expirationTime(expiration)
                        .regenerado(false)
                        .build();
            }

            return TokenValidationResponse.builder()
                    .activo(false)
                    .mensaje("Token vencido"
                            + (expiration != null ? ". Expiró el " + sdf.format(expiration) : ""))
                    .expirationTime(expiration)
                    .regenerado(false)
                    .build();
        } catch (Exception e) {
            return TokenValidationResponse.builder()
                    .activo(false)
                    .mensaje("Error al leer token: " + e.getMessage())
                    .regenerado(false)
                    .build();
        }
    }

    public TokenValidationResponse validarYRegenerarToken() {
        if (!afipProperties.isEnabled()) {
            return TokenValidationResponse.builder()
                    .activo(true)
                    .mensaje("Módulo AFIP deshabilitado")
                    .regenerado(false)
                    .build();
        }

        try {
            File taXmlFile = resolverArchivoToken();

            if (taXmlFile == null || !taXmlFile.exists()) {
                return regenerarToken("El archivo TA.xml no existe. Generando nuevo token...");
            }

            AFIPTokenLoader.TokenSign tokenSign = AFIPTokenLoader.loadFromXml(taXmlFile.getAbsolutePath());
            if (tokenSign.isValid()) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                return TokenValidationResponse.builder()
                        .activo(true)
                        .mensaje("Token AFIP activo. Expira el " + sdf.format(tokenSign.getExpiration()))
                        .expirationTime(tokenSign.getExpiration())
                        .regenerado(false)
                        .build();
            }

            return regenerarToken("Token AFIP vencido o próximo a vencer. Regenerando...");
        } catch (Exception e) {
            log.error("Error al validar token AFIP: {}", e.getMessage());
            return TokenValidationResponse.builder()
                    .activo(false)
                    .mensaje("Error al validar token: " + e.getMessage())
                    .regenerado(false)
                    .build();
        }
    }

    private TokenValidationResponse regenerarToken(String motivo) {
        try {
            String phpScriptPath = AfipTokenPathResolver.resolvePhpScriptPath(afipProperties);
            if (phpScriptPath == null || phpScriptPath.isBlank()) {
                return TokenValidationResponse.builder()
                        .activo(false)
                        .mensaje(motivo + " Configure afip.php-script-path"
                                + (afipProperties.isHomologacion() ? "-homologacion" : "")
                                + " para regeneración automática.")
                        .regenerado(false)
                        .build();
            }

            File phpScript = new File(phpScriptPath);
            if (!phpScript.exists()) {
                return TokenValidationResponse.builder()
                        .activo(false)
                        .mensaje("No se encontró wsaa-client.php en: " + phpScriptPath)
                        .regenerado(false)
                        .build();
            }

            log.info("Regenerando token AFIP con script PHP: {}", phpScript.getAbsolutePath());

            ProcessBuilder processBuilder = new ProcessBuilder();
            String os = System.getProperty("os.name").toLowerCase();
            String phpCommand = os.contains("win") ? "php.exe" : "php";
            processBuilder.command(phpCommand, phpScript.getAbsolutePath(), SERVICE_NAME);
            processBuilder.directory(phpScript.getParentFile());
            processBuilder.redirectErrorStream(true);
            processBuilder.environment().put("AFIP_HOMOLOGACION", afipProperties.isHomologacion() ? "1" : "0");

            Process process;
            try {
                process = processBuilder.start();
            } catch (IOException e) {
                if (os.contains("win")) {
                    processBuilder.command("php", phpScript.getAbsolutePath(), SERVICE_NAME);
                    process = processBuilder.start();
                } else {
                    throw e;
                }
            }

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append('\n');
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("Script PHP falló ({}): {}", exitCode, output);
                return TokenValidationResponse.builder()
                        .activo(false)
                        .mensaje("Error al ejecutar PHP. Código: " + exitCode + ". Salida: " + output)
                        .regenerado(false)
                        .build();
            }

            File taGenerado = new File(phpScript.getParentFile(), "TA.xml");
            if (!taGenerado.exists()) {
                taGenerado = resolverArchivoToken();
            }
            if (taGenerado == null || !taGenerado.exists()) {
                return TokenValidationResponse.builder()
                        .activo(false)
                        .mensaje("El script PHP se ejecutó pero no se generó TA.xml")
                        .regenerado(true)
                        .build();
            }

            sincronizarToken(taGenerado);
            wsaaService.limpiarCache();

            AFIPTokenLoader.TokenSign tokenSign = AFIPTokenLoader.loadFromXml(taGenerado.getAbsolutePath());
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

            log.info("Token AFIP regenerado. Expira: {}", tokenSign.getExpiration());

            return TokenValidationResponse.builder()
                    .activo(tokenSign.isValid())
                    .mensaje(tokenSign.isValid()
                            ? "Token regenerado exitosamente. Expira el " + sdf.format(tokenSign.getExpiration())
                            : "Token regenerado pero ya expiró o es inválido")
                    .expirationTime(tokenSign.getExpiration())
                    .regenerado(true)
                    .build();
        } catch (Exception e) {
            log.error("Error al regenerar token AFIP: {}", e.getMessage(), e);
            return TokenValidationResponse.builder()
                    .activo(false)
                    .mensaje("Error al regenerar token: " + e.getMessage())
                    .regenerado(false)
                    .build();
        }
    }

    private File resolverArchivoToken() {
        return AfipTokenPathResolver.resolve(afipProperties);
    }

    private void sincronizarToken(File taOrigen) throws IOException {
        List<Path> destinos = new ArrayList<>();

        if (afipProperties.isHomologacion()) {
            destinos.add(Path.of("src/main/resources/certificados/TA-homologacion.xml"));
        } else {
            destinos.add(Path.of("src/main/resources/certificados/TA.xml"));
        }

        File certDir = AfipTokenPathResolver.resolveCertificadosDir(afipProperties);
        if (certDir != null) {
            destinos.add(new File(certDir, "TA.xml").toPath());
        }

        for (Path destino : destinos) {
            if (destino.toAbsolutePath().normalize().equals(taOrigen.toPath().toAbsolutePath().normalize())) {
                continue;
            }
            if (destino.getParent() != null) {
                Files.createDirectories(destino.getParent());
            }
            Files.copy(taOrigen.toPath(), destino, StandardCopyOption.REPLACE_EXISTING);
            log.info("TA.xml sincronizado en {}", destino.toAbsolutePath());
        }
    }
}
