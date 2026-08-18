package com.vida.apirest.tenant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantRequestBinderTest {

    @Test
    void loginUsaSoloElHeader() {
        var r = TenantRequestBinder.resolve("EMPRESA-A", "EMPRESA-B", true, true);
        assertTrue(r.ok());
        assertEquals("EMPRESA-A", r.codigo());
    }

    @Test
    void loginSinHeaderFalla() {
        var r = TenantRequestBinder.resolve(null, null, false, true);
        assertEquals(TenantRequestBinder.Error.MISSING_HEADER, r.error());
    }

    @Test
    void requestAutenticadoUsaClaimAunqueHayaHeaderIgual() {
        var r = TenantRequestBinder.resolve("EMPRESA-A", "EMPRESA-A", true, false);
        assertTrue(r.ok());
        assertEquals("EMPRESA-A", r.codigo());
    }

    @Test
    void requestAutenticadoSinHeaderUsaClaim() {
        var r = TenantRequestBinder.resolve(null, "EMPRESA-A", true, false);
        assertTrue(r.ok());
        assertEquals("EMPRESA-A", r.codigo());
    }

    @Test
    void headerDistintoAlClaimSeRechaza() {
        var r = TenantRequestBinder.resolve("EMPRESA-B", "EMPRESA-A", true, false);
        assertEquals(TenantRequestBinder.Error.MISMATCH, r.error());
        assertNull(r.codigo());
    }

    @Test
    void bearerSinClaimNoCaeAlHeader() {
        var r = TenantRequestBinder.resolve("EMPRESA-B", null, true, false);
        assertEquals(TenantRequestBinder.Error.MISSING_CLAIM, r.error());
    }

    @Test
    void sinBearerUsaHeader() {
        var r = TenantRequestBinder.resolve(" EMPRESA-A ", null, false, false);
        assertTrue(r.ok());
        assertEquals("EMPRESA-A", r.codigo());
    }
}
