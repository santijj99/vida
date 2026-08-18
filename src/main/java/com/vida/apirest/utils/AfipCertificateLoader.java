package com.vida.apirest.utils;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;

import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

public final class AfipCertificateLoader {

    private static final int MAX_CERT_BYTES = 2 * 1024 * 1024;

    public record AfipCredentials(
            PrivateKey privateKey,
            X509Certificate certificate,
            List<X509Certificate> chain) {
    }

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private AfipCertificateLoader() {
    }

    /**
     * Resuelve credenciales AFIP desde la carpeta de la empresa.
     * Soporta el formato PHP (certificado.crt + MiClavePrivada.key) o PKCS#12 (.p12) como el cliente oficial AFIP.
     */
    public static AfipCredentials resolve(Path certDir, String password) throws Exception {
        Path p12 = findPkcs12(certDir);
        if (p12 != null) {
            return loadPkcs12(p12, password);
        }

        Path certPath = certDir.resolve("certificado.crt");
        Path keyPath = certDir.resolve("MiClavePrivada.key");
        if (!Files.isRegularFile(certPath) || !Files.isRegularFile(keyPath)) {
            throw new IllegalStateException(
                    "Certificados AFIP no encontrados en " + certDir
                            + ". Se requieren certificado.crt + MiClavePrivada.key o un archivo .p12");
        }

        List<X509Certificate> chain = loadCertificateChain(certPath);
        PrivateKey privateKey = loadPrivateKey(keyPath, password);
        return new AfipCredentials(privateKey, chain.get(0), chain);
    }

    public static boolean esCertificadoHomologacion(X509Certificate cert) {
        String issuer = cert.getIssuerX500Principal().getName().toLowerCase(Locale.ROOT);
        return issuer.contains("test") || issuer.contains("computadores test");
    }

    public static void validarCertificadoParaAmbiente(X509Certificate cert, boolean homologacion) {
        boolean esHomo = esCertificadoHomologacion(cert);
        String issuer = cert.getIssuerX500Principal().getName();
        if (homologacion && !esHomo) {
            throw new IllegalStateException(
                    "El certificado es de PRODUCCIÓN (emisor: " + issuer + ") pero el ambiente está en HOMOLOGACIÓN. "
                            + "Usá el certificado de testing (emisor «Computadores Test») o cambiá el ambiente a Producción.");
        }
        if (!homologacion && esHomo) {
            throw new IllegalStateException(
                    "El certificado es de HOMOLOGACIÓN/TESTING (emisor: " + issuer + ") pero el ambiente está en PRODUCCIÓN. "
                            + "Usá el certificado de producción o cambiá el ambiente a Homologación.");
        }
    }

    public static List<X509Certificate> loadCertificateChain(Path certPath) throws Exception {
        List<X509Certificate> chain = new ArrayList<>();
        JcaX509CertificateConverter converter = new JcaX509CertificateConverter()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME);

        try (InputStream is = Files.newInputStream(certPath);
             PEMParser parser = new PEMParser(new InputStreamReader(is))) {
            Object object;
            while ((object = parser.readObject()) != null) {
                if (object instanceof X509CertificateHolder holder) {
                    chain.add(converter.getCertificate(holder));
                }
            }
        }

        if (chain.isEmpty()) {
            throw new IllegalStateException("No se encontraron certificados X.509 en " + certPath);
        }
        return chain;
    }

    public static X509Certificate loadCertificate(Path certPath) throws Exception {
        return loadCertificateChain(certPath).get(0);
    }

    public static PrivateKey loadPrivateKey(Path keyPath, String password) throws Exception {
        try (InputStream is = Files.newInputStream(keyPath)) {
            return loadPrivateKey(is, password, keyPath.toString());
        }
    }

    public static void validarContenidoPkcs12(byte[] bytes, String password) throws Exception {
        exigirTamano(bytes, "PKCS#12");
        char[] pass = password != null ? password.toCharArray() : new char[0];
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream is = new ByteArrayInputStream(bytes)) {
            ks.load(is, pass);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "El archivo no es un PKCS#12 válido o la contraseña es incorrecta");
        }
        Enumeration<String> aliases = ks.aliases();
        while (aliases.hasMoreElements()) {
            if (ks.isKeyEntry(aliases.nextElement())) {
                return;
            }
        }
        throw new IllegalArgumentException("El PKCS#12 no contiene una clave privada");
    }

    public static void validarContenidoCertificado(byte[] bytes) throws Exception {
        exigirTamano(bytes, "certificado");
        String ascii = new String(bytes, 0, Math.min(bytes.length, 256), StandardCharsets.US_ASCII);
        if (ascii.contains("BEGIN CERTIFICATE REQUEST") || ascii.contains("BEGIN NEW CERTIFICATE REQUEST")) {
            throw new IllegalArgumentException(
                    "Subiste un CSR. AFIP necesita el certificado emitido (.crt), no el pedido de firma.");
        }
        try (InputStream is = new ByteArrayInputStream(bytes);
             PEMParser parser = new PEMParser(new InputStreamReader(is, StandardCharsets.US_ASCII))) {
            Object object;
            boolean found = false;
            while ((object = parser.readObject()) != null) {
                if (object instanceof PKCS10CertificationRequest) {
                    throw new IllegalArgumentException(
                            "Subiste un CSR. AFIP necesita el certificado emitido (.crt), no el pedido de firma.");
                }
                if (object instanceof X509CertificateHolder) {
                    found = true;
                }
            }
            if (found) {
                return;
            }
        }
        try (InputStream is = new ByteArrayInputStream(bytes)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            if (!(cf.generateCertificate(is) instanceof X509Certificate)) {
                throw new IllegalArgumentException("El archivo no es un certificado X.509");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("El archivo no es un certificado X.509/PEM válido");
        }
    }

    public static void validarContenidoClavePrivada(byte[] bytes, String password) throws Exception {
        exigirTamano(bytes, "clave privada");
        try (InputStream is = new ByteArrayInputStream(bytes)) {
            loadPrivateKey(is, password, "clave privada");
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "La clave privada no es PEM válida o la contraseña es incorrecta");
        }
    }

    private static void exigirTamano(byte[] bytes, String que) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("El archivo de " + que + " está vacío");
        }
        if (bytes.length > MAX_CERT_BYTES) {
            throw new IllegalArgumentException("El archivo de " + que + " supera 2 MB");
        }
    }

    private static PrivateKey loadPrivateKey(InputStream is, String password, String origen) throws Exception {
        try (PEMParser parser = new PEMParser(new InputStreamReader(is, StandardCharsets.US_ASCII))) {
            Object object = parser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter()
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME);

            if (object instanceof PEMEncryptedKeyPair encrypted) {
                char[] pass = password != null ? password.toCharArray() : new char[0];
                PEMKeyPair keyPair = encrypted.decryptKeyPair(
                        new JcePEMDecryptorProviderBuilder()
                                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                                .build(pass));
                return converter.getPrivateKey(keyPair.getPrivateKeyInfo());
            }
            if (object instanceof PEMKeyPair keyPair) {
                return converter.getPrivateKey(keyPair.getPrivateKeyInfo());
            }
            if (object instanceof PrivateKeyInfo privateKeyInfo) {
                return converter.getPrivateKey(privateKeyInfo);
            }
            throw new IllegalStateException("Formato de clave privada no soportado: " + origen);
        }
    }

    public static AfipCredentials loadPkcs12(Path p12Path, String password) throws Exception {
        char[] pass = password != null ? password.toCharArray() : new char[0];
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream is = Files.newInputStream(p12Path)) {
            ks.load(is, pass);
        }

        String alias = null;
        Enumeration<String> aliases = ks.aliases();
        while (aliases.hasMoreElements()) {
            String candidate = aliases.nextElement();
            if (ks.isKeyEntry(candidate)) {
                alias = candidate;
                break;
            }
        }
        if (alias == null) {
            throw new IllegalStateException("El archivo PKCS#12 no contiene una clave privada: " + p12Path);
        }

        PrivateKey privateKey = (PrivateKey) ks.getKey(alias, pass);
        X509Certificate certificate = (X509Certificate) ks.getCertificate(alias);
        List<X509Certificate> chain = new ArrayList<>();
        java.security.cert.Certificate[] certChain = ks.getCertificateChain(alias);
        if (certChain != null) {
            for (java.security.cert.Certificate c : certChain) {
                chain.add((X509Certificate) c);
            }
        }
        if (chain.isEmpty()) {
            chain.add(certificate);
        }
        return new AfipCredentials(privateKey, certificate, chain);
    }

    private static Path findPkcs12(Path certDir) throws Exception {
        Path preferred = certDir.resolve("certificado.p12");
        if (Files.isRegularFile(preferred)) {
            return preferred;
        }
        try (var stream = Files.list(certDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".p12"))
                    .findFirst()
                    .orElse(null);
        }
    }
}
