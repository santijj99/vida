package com.vida.apirest.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordResetCodesTest {

    @Test
    void generateSiempre6Digitos() {
        for (int i = 0; i < 50; i++) {
            String codigo = PasswordResetCodes.generate6Digits();
            assertEquals(6, codigo.length());
            assertTrue(codigo.chars().allMatch(Character::isDigit));
        }
    }

    @Test
    void hashNoEsElCodigoEnClaro() {
        String hash = PasswordResetCodes.hash("123456");
        assertEquals(64, hash.length());
        assertFalse(hash.contains("123456"));
        assertTrue(PasswordResetCodes.matches("123456", hash));
        assertFalse(PasswordResetCodes.matches("123457", hash));
        assertFalse(PasswordResetCodes.matches("123456", "123456"));
    }
}
