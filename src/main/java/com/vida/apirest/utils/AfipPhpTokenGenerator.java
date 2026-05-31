package com.vida.apirest.utils;

import com.vida.apirest.config.AfipProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

@Component
@RequiredArgsConstructor
public class AfipPhpTokenGenerator {

    private static final Logger log = LoggerFactory.getLogger(AfipPhpTokenGenerator.class);

    private final AfipProperties afipProperties;

    public AFIPTokenLoader.TokenSign generarToken(String service, String outputFileName) throws Exception {
        String phpScriptPath = AfipTokenPathResolver.resolvePhpScriptPath(afipProperties);
        if (phpScriptPath == null || phpScriptPath.isBlank()) {
            throw new IllegalStateException("No está configurado el script PHP de AFIP");
        }

        File phpScript = new File(phpScriptPath);
        if (!phpScript.exists()) {
            throw new IllegalStateException("No se encontró wsaa-client.php en: " + phpScriptPath);
        }

        String outName = outputFileName != null && !outputFileName.isBlank() ? outputFileName : "TA.xml";
        log.info("Generando token AFIP servicio {} → {}", service, outName);

        ProcessBuilder processBuilder = new ProcessBuilder();
        String os = System.getProperty("os.name").toLowerCase();
        String phpCommand = os.contains("win") ? "php.exe" : "php";
        processBuilder.command(phpCommand, phpScript.getAbsolutePath(), service, outName);
        processBuilder.directory(phpScript.getParentFile());
        processBuilder.redirectErrorStream(true);
        processBuilder.environment().put("AFIP_HOMOLOGACION", afipProperties.isHomologacion() ? "1" : "0");

        Process process;
        try {
            process = processBuilder.start();
        } catch (IOException e) {
            if (os.contains("win")) {
                processBuilder.command("php", phpScript.getAbsolutePath(), service, outName);
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
        String salida = output.toString().trim();

        if (salida.contains("SOAP Fault") || salida.contains("Servicio informado inexistente")
                || salida.contains("Computador no autorizado")) {
            throw new IllegalStateException(traducirErrorToken(service, salida));
        }

        if (exitCode != 0) {
            throw new IllegalStateException("Error al generar token AFIP (" + service + "): " + salida);
        }

        File taFile = new File(phpScript.getParentFile(), outName);
        if (!taFile.exists()) {
            throw new IllegalStateException(traducirErrorToken(service, salida.isEmpty()
                    ? "No se generó " + outName
                    : salida));
        }

        return AFIPTokenLoader.loadFromXml(taFile.getAbsolutePath());
    }

    private String traducirErrorToken(String service, String salida) {
        if (salida.contains("Computador no autorizado") || salida.contains("notAuthorized")) {
            return "El certificado AFIP no tiene autorizado el servicio de padrón. "
                    + "Habilitalo en AFIP → Administrador de Relaciones → Web Services → "
                    + "«Servicio Consulta Padron A13» (ws_sr_padron_a13), con el alias certificado.";
        }
        if (salida.contains("Servicio informado inexistente") || salida.contains("wsn.notFound")) {
            return "Servicio WSAA inválido: " + service + ". Verificá el nombre del servicio de padrón.";
        }
        if (salida.contains("token ha expirado") || salida.contains("expirado")) {
            return "Token AFIP expirado. Regenerá el certificado/token e intentá de nuevo.";
        }
        return "Error al generar token AFIP (" + service + "): " + salida;
    }
}
