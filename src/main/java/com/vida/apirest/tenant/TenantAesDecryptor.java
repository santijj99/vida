package com.vida.apirest.tenant;

import com.vida.apirest.config.LicenciaProperties;
import com.vida.apirest.security.AesGcmCodec;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;

/**
 * Descifra passwords de conexiones del servidor de licencias (AES-GCM, misma clave).
 */
@Component
public class TenantAesDecryptor {

    private final SecretKeySpec secretKey;

    public TenantAesDecryptor(LicenciaProperties properties) {
        this.secretKey = AesGcmCodec.keyFromUtf8(properties.getAesKey());
    }

    public String decrypt(String encrypted) {
        try {
            return AesGcmCodec.decrypt(secretKey, encrypted);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("No se pudo descifrar la password de conexión del tenant", ex);
        }
    }
}
