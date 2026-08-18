package com.vida.apirest.controller;

import com.vida.apirest.dto.afip.EmitirFacturaAFIPRequest;
import com.vida.apirest.dto.ariticulo.ArticuloCreateRequest;
import com.vida.apirest.dto.sistema.SistemaInfoResponse;
import com.vida.apirest.dto.venta.AbrirCajaRequest;
import com.vida.apirest.dto.venta.VentaCreateRequest;
import com.vida.apirest.security.Authz;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MutationPreAuthorizeContractTest {

    @Test
    void crearArticuloExigeGestionarNoSoloVer() throws Exception {
        assertPreAuthorize(
                ArticuloController.class.getMethod("createArticulo", ArticuloCreateRequest.class),
                Authz.GESTIONAR_ARTICULOS);
        PreAuthorize typeLevel = ArticuloController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(typeLevel);
        assertEquals(Authz.VER_O_GESTIONAR_ARTICULOS, typeLevel.value());
    }

    @Test
    void registrarVentaExigeGestionar() throws Exception {
        assertPreAuthorize(
                VentaController.class.getMethod("registrarVenta", VentaCreateRequest.class),
                Authz.GESTIONAR_VENTAS);
    }

    @Test
    void abrirCajaExigeGestionar() throws Exception {
        assertPreAuthorize(
                VentaController.class.getMethod("abrirCaja", AbrirCajaRequest.class),
                Authz.GESTIONAR_CAJA);
    }

    @Test
    void emitirAfipExigeGestionar() throws Exception {
        assertPreAuthorize(
                FacturaAFIPController.class.getMethod("emitir", Long.class, EmitirFacturaAFIPRequest.class),
                Authz.GESTIONAR_ARCA);
    }

    @Test
    void validarLicenciaExigeGestionar() throws Exception {
        Method method = SistemaController.class.getMethod(
                "validarAhora", jakarta.servlet.http.HttpServletRequest.class);
        assertPreAuthorize(method, Authz.GESTIONAR_ORGANIZACION);
        assertNotNull(method.getAnnotation(PostMapping.class));
    }

    private static void assertPreAuthorize(Method method, String expected) {
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, () -> method.getName() + " sin @PreAuthorize");
        assertEquals(expected, preAuthorize.value());
        assertTrue(preAuthorize.value().contains("GESTIONAR_") || preAuthorize.value().contains("hasAuthority"),
                () -> method.getName() + " no exige mutación: " + preAuthorize.value());
    }
}
