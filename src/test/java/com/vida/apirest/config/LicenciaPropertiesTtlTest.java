package com.vida.apirest.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LicenciaPropertiesTtlTest {

    @Test
    void revalidacionOnlineUsaMinutosYNuncaCero() {
        LicenciaProperties p = new LicenciaProperties();
        p.setCacheMinutos(360);
        assertEquals(Duration.ofMinutes(360), p.revalidacionOnline());
        p.setCacheMinutos(0);
        assertEquals(Duration.ofMinutes(1), p.revalidacionOnline());
    }

    @Test
    void graciaOfflineEsPorDias() {
        LicenciaProperties p = new LicenciaProperties();
        p.setGraciaDias(20);
        assertEquals(Duration.ofDays(20), p.graciaOffline());
        assertTrue(p.revalidacionOnline().compareTo(p.graciaOffline()) < 0);
    }
}
