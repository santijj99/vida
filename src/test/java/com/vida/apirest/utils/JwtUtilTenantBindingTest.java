package com.vida.apirest.utils;

import com.vida.apirest.config.JwtProperties;
import com.vida.apirest.model.auth.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTenantBindingTest {

    private JwtUtil jwtUtil;
    private Usuario usuario;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("dev-only-change-me-before-production-min-32-chars");
        props.setExpirationHours(24);
        jwtUtil = new JwtUtil(props);

        usuario = new Usuario();
        usuario.setUsuario("admin");
        userDetails = User.withUsername("admin").password("x").roles("ADMINISTRADOR").build();
    }

    @Test
    void tokenValidoSiClaimCoincideConTenant() {
        String token = jwtUtil.generateToken(usuario, List.of(), List.of(), "EMPRESA-A");
        assertTrue(jwtUtil.isTokenValid(token, userDetails, "EMPRESA-A"));
    }

    @Test
    void tokenInvalidoSiClaimNoCoincideConTenant() {
        String token = jwtUtil.generateToken(usuario, List.of(), List.of(), "EMPRESA-A");
        assertFalse(jwtUtil.isTokenValid(token, userDetails, "EMPRESA-B"));
    }

    @Test
    void tokenSinClaimInvalidoCuandoHayTenantEsperado() {
        String token = jwtUtil.generateToken(usuario, List.of(), List.of(), null);
        assertFalse(jwtUtil.isTokenValid(token, userDetails, "EMPRESA-A"));
    }

    @Test
    void sinTenantEsperadoSoloChequeaUsuarioYExpiracion() {
        String token = jwtUtil.generateToken(usuario, List.of(), List.of(), "EMPRESA-A");
        assertTrue(jwtUtil.isTokenValid(token, userDetails, null));
    }
}
