package com.vida.apirest.utils;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AfipCertificateLoaderValidationTest {

    @Test
    void rechazaHtmlComoPkcs12() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> AfipCertificateLoader.validarContenidoPkcs12(
                        "<html>no es un p12</html>".getBytes(StandardCharsets.UTF_8),
                        "secret"));
        assertTrue(ex.getMessage().toLowerCase().contains("pkcs"));
    }

    @Test
    void rechazaCsrComoCertificado() {
        byte[] csr = """
                -----BEGIN CERTIFICATE REQUEST-----
                MIIBVjCB/QIBADANMQswCQYDVQQDDAJ4eA==
                -----END CERTIFICATE REQUEST-----
                """.getBytes(StandardCharsets.US_ASCII);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> AfipCertificateLoader.validarContenidoCertificado(csr));
        assertTrue(ex.getMessage().contains("CSR"));
    }

    @Test
    void rechazaClaveVacia() {
        assertThrows(IllegalArgumentException.class,
                () -> AfipCertificateLoader.validarContenidoClavePrivada(new byte[0], null));
    }

    @Test
    void rechazaTextoPlanoComoCertificado() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> AfipCertificateLoader.validarContenidoCertificado(
                        "esto no es un certificado".getBytes(StandardCharsets.UTF_8)));
        assertTrue(ex.getMessage().toLowerCase().contains("certificado")
                || ex.getMessage().toLowerCase().contains("x.509"));
    }

    @Test
    void aceptaPemAutofirmado() throws Exception {
        KeyPair kp = rsaKeyPair();
        byte[] certPem = toPem("CERTIFICATE", selfSigned(kp).getEncoded());
        byte[] keyPem = toPem("PRIVATE KEY", kp.getPrivate().getEncoded());
        assertDoesNotThrow(() -> AfipCertificateLoader.validarContenidoCertificado(certPem));
        assertDoesNotThrow(() -> AfipCertificateLoader.validarContenidoClavePrivada(keyPem, null));
    }

    private static KeyPair rsaKeyPair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        return kpg.generateKeyPair();
    }

    private static X509CertificateHolder selfSigned(KeyPair kp) throws Exception {
        X500Name name = new X500Name("CN=test");
        Date now = new Date();
        Date until = new Date(now.getTime() + 86_400_000L);
        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                name, BigInteger.ONE, now, until, name, kp.getPublic());
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(kp.getPrivate());
        return builder.build(signer);
    }

    private static byte[] toPem(String type, byte[] der) {
        String body = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(der);
        return ("-----BEGIN " + type + "-----\n" + body + "\n-----END " + type + "-----\n")
                .getBytes(StandardCharsets.US_ASCII);
    }
}
