package com.vida.apirest.controller;

import com.vida.apirest.dto.ariticulo.ArticuloCompactResponse;
import com.vida.apirest.dto.ariticulo.ArticuloCreateRequest;
import com.vida.apirest.dto.venta.AbrirCajaRequest;
import com.vida.apirest.dto.venta.VentaCreateRequest;
import com.vida.apirest.dto.venta.VentaResponse;
import com.vida.apirest.model.articulo.Articulo;
import com.vida.apirest.security.Authz;
import com.vida.apirest.servicies.ArticuloService;
import com.vida.apirest.servicies.CajaSesionService;
import com.vida.apirest.servicies.EmpleadoService;
import com.vida.apirest.servicies.VentaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MutationsPreAuthorizeTest {

    private AnnotationConfigApplicationContext context;
    private ArticuloController articuloController;
    private VentaController ventaController;
    private ArticuloService articuloService;
    private VentaService ventaService;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(Config.class);
        articuloController = context.getBean(ArticuloController.class);
        ventaController = context.getBean(VentaController.class);
        articuloService = context.getBean(ArticuloService.class);
        ventaService = context.getBean(VentaService.class);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
            if (context != null) {
                context.close();
            }
    }

    @Test
    void verArticulosNoCrea() {
        authenticate("VER_ARTICULOS");
        assertThrows(AccessDeniedException.class,
                () -> articuloController.createArticulo(new ArticuloCreateRequest()));
    }

    @Test
    void gestionarArticulosCrea() {
        authenticate("GESTIONAR_ARTICULOS");
        Articulo articulo = new Articulo();
        articulo.setId(1L);
        when(articuloService.createArticulo(any())).thenReturn(articulo);
        when(articuloService.getCompactById(1L)).thenReturn(new ArticuloCompactResponse());

        assertEquals(HttpStatus.CREATED,
                articuloController.createArticulo(new ArticuloCreateRequest()).getStatusCode());
    }

    @Test
    void verVentasNoRegistra() {
        authenticate("VER_VENTAS");
        assertThrows(AccessDeniedException.class,
                () -> ventaController.registrarVenta(new VentaCreateRequest()));
    }

    @Test
    void gestionarVentasRegistra() {
        authenticate("GESTIONAR_VENTAS");
        when(ventaService.registrarVenta(any())).thenReturn(new VentaResponse());
        assertEquals(HttpStatus.CREATED,
                ventaController.registrarVenta(new VentaCreateRequest()).getStatusCode());
    }

    @Test
    void verCajaNoAbreSesion() {
        authenticate("VER_CAJA");
        assertThrows(AccessDeniedException.class,
                () -> ventaController.abrirCaja(new AbrirCajaRequest()));
    }

    @Test
    void expresionesDeMutacionNoSonSoloVer() {
        assertEquals("hasAuthority('GESTIONAR_ARTICULOS')", Authz.GESTIONAR_ARTICULOS);
        assertEquals("hasAuthority('GESTIONAR_VENTAS')", Authz.GESTIONAR_VENTAS);
        assertEquals("hasAuthority('GESTIONAR_CAJA')", Authz.GESTIONAR_CAJA);
        assertEquals("hasAuthority('GESTIONAR_ARCA')", Authz.GESTIONAR_ARCA);
        assertEquals("hasAuthority('GESTIONAR_ORGANIZACION')", Authz.GESTIONAR_ORGANIZACION);
    }

    private static void authenticate(String authority) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "tester",
                        "n/a",
                        List.of(new SimpleGrantedAuthority(authority))));
    }

    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean
        ArticuloService articuloService() {
            return mock(ArticuloService.class);
        }

        @Bean
        ArticuloController articuloController(ArticuloService articuloService) {
            return new ArticuloController(articuloService);
        }

        @Bean
        VentaService ventaService() {
            return mock(VentaService.class);
        }

        @Bean
        CajaSesionService cajaSesionService() {
            return mock(CajaSesionService.class);
        }

        @Bean
        VentaController ventaController(VentaService ventaService, CajaSesionService cajaSesionService) {
            // mock local: si es bean Spring intenta inyectar @Autowired del EmpleadoService real.
            return new VentaController(ventaService, cajaSesionService, mock(EmpleadoService.class));
        }
    }
}
