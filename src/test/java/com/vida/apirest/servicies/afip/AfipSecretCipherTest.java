package com.vida.apirest.servicies.afip;

import com.vida.apirest.config.LicenciaProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AfipSecretCipherTest {

    private final AfipSecretCipher cipher = new AfipSecretCipher(props("12345678901234567890123456789012"));

    @Test
    void roundTripNoQuedaEnClaro() {
        String stored = cipher.encryptForStorage("mi-password-p12");
        assertTrue(stored.startsWith("AESGCM:"));
        assertFalse(stored.contains("mi-password-p12"));
        assertEquals("mi-password-p12", cipher.decryptToPlain(stored));
    }

    @Test
    void legadoEnClaroSigueLeyendo() {
        assertEquals("vieja", cipher.decryptToPlain("vieja"));
        assertFalse(cipher.isWrapped("vieja"));
    }

    @Test
    void vacioEsNull() {
        assertNull(cipher.encryptForStorage("  "));
        assertNull(cipher.decryptToPlain(null));
    }

    @Test
    void payloadCorruptoFalla() {
        assertThrows(IllegalStateException.class,
                () -> cipher.decryptToPlain("AESGCM:no-es-base64-valido!!!"));
    }

    @Test
    void dosCifradosDelMismoValorDifierenPorElIv() {
        assertNotEquals(
                cipher.encryptForStorage("misma"),
                cipher.encryptForStorage("misma"));
    }

    private static LicenciaProperties props(String aesKey) {
        LicenciaProperties p = new LicenciaProperties();
        p.setAesKey(aesKey);
        return p;
    }
}
