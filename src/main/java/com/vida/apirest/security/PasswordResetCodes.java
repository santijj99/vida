package com.vida.apirest.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Códigos de recuperación: 6 dígitos con {@link SecureRandom}, persistidos como SHA-256.
 */
public final class PasswordResetCodes {

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordResetCodes() {
    }

    public static String generate6Digits() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    public static String hash(String codigo) {
        if (codigo == null) {
            throw new IllegalArgumentException("codigo");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(
                    digest.digest(codigo.trim().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }

    public static boolean matches(String codigoPlano, String hashGuardado) {
        if (codigoPlano == null || hashGuardado == null || hashGuardado.isBlank()) {
            return false;
        }
        byte[] expected = hash(codigoPlano).getBytes(StandardCharsets.UTF_8);
        byte[] actual = hashGuardado.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }
}
