package com.vida.apirest.utils;

import com.vida.apirest.config.AfipProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;

/**
 * Resuelve rutas de certificados y TA.xml según el ambiente (homologación vs producción).
 */
public final class AfipTokenPathResolver {

    private static final Logger log = LoggerFactory.getLogger(AfipTokenPathResolver.class);

    private AfipTokenPathResolver() {
    }

    public static String resolvePhpScriptPath(AfipProperties afipProperties) {
        if (afipProperties.isHomologacion()) {
            String homo = afipProperties.getPhpScriptPathHomologacion();
            if (homo != null && !homo.isBlank()) {
                return homo;
            }
        }
        return afipProperties.getPhpScriptPath();
    }

    public static File resolveCertificadosDir(AfipProperties afipProperties) {
        String phpScript = resolvePhpScriptPath(afipProperties);
        if (phpScript != null && !phpScript.isBlank()) {
            File parent = new File(phpScript).getParentFile();
            if (parent != null && parent.isDirectory()) {
                return parent;
            }
        }
        return null;
    }

    public static File resolve(AfipProperties afipProperties) {
        File certDir = resolveCertificadosDir(afipProperties);
        if (certDir != null) {
            File ta = new File(certDir, "TA.xml");
            if (ta.exists()) {
                return ta;
            }
        }

        String tokenPath = afipProperties.getTokenXmlPath();
        if (tokenPath != null && !tokenPath.isBlank()) {
            if (tokenPath.startsWith("classpath:")) {
                try {
                    ClassPathResource resource = new ClassPathResource(tokenPath.substring("classpath:".length()));
                    if (resource.exists()) {
                        return resource.getFile();
                    }
                } catch (IOException e) {
                    log.debug("Token classpath no accesible como archivo: {}", e.getMessage());
                }
            } else {
                File file = new File(tokenPath);
                if (file.exists()) {
                    return file;
                }
            }
        }

        File taEnResources = new File("src/main/resources/certificados/TA.xml");
        if (taEnResources.exists()) {
            return taEnResources;
        }

        return null;
    }

    public static String resolveLoadPath(AfipProperties afipProperties) {
        File file = resolve(afipProperties);
        return file != null ? file.getAbsolutePath() : afipProperties.getTokenXmlPath();
    }
}
