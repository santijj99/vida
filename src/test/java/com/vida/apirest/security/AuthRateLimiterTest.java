package com.vida.apirest.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthRateLimiterTest {

    @Test
    void bloqueaAlSuperarElMaximo() {
        AuthRateLimiter limiter = new AuthRateLimiter();
        assertTrue(limiter.tryConsume("k", 3));
        assertTrue(limiter.tryConsume("k", 3));
        assertTrue(limiter.tryConsume("k", 3));
        assertFalse(limiter.tryConsume("k", 3));
    }

    @Test
    void resetLiberaLaVentana() {
        AuthRateLimiter limiter = new AuthRateLimiter();
        limiter.tryConsume("k", 1);
        assertFalse(limiter.tryConsume("k", 1));
        limiter.reset("k");
        assertTrue(limiter.tryConsume("k", 1));
    }
}
