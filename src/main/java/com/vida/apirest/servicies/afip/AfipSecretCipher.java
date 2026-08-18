package com.vida.apirest.servicies.afip;

import com.vida.apirest.config.LicenciaProperties;
import com.vida.apirest.security.AesGcmCodec;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;

/**
 * Cifra la password del certificado AFIP en {@code empresa_afip_config}.
 * Prefijo {@code AESGCM:} para distinguir valores viejos en claro.
 */
@Component
public class AfipSecretCipher {

    static final String PREFIX = "AESGCM:";

    private final SecretKeySpec secretKey;

    public AfipSecretCipher(LicenciaProperties properties) {
        this.secretKey = AesGcmCodec.keyFromUtf8(properties.getAesKey());
    }

    public boolean isWrapped(String stored) {
        return stored != null && stored.startsWith(PREFIX);
    }

    public String encryptForStorage(String plain) {
        if (plain == null || plain.isBlank()) {
            return null;
        }
        if (isWrapped(plain)) {
            return plain;
        }
        return PREFIX + AesGcmCodec.encrypt(secretKey, plain);
    }

    public String decryptToPlain(String stored) {
        if (stored == null || stored.isBlank()) {
            return null;
        }
        if (!isWrapped(stored)) {
            return stored;
        }
        try {
            return AesGcmCodec.decrypt(secretKey, stored.substring(PREFIX.length()));
        } catch (RuntimeException ex) {
            throw new IllegalStateException("No se pudo descifrar la clave del certificado AFIP", ex);
        }
    }
}
