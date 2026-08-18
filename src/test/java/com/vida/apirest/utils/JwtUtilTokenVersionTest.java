package com.vida.apirest.utils;

import com.vida.apirest.config.JwtProperties;
import com.vida.apirest.model.auth.Usuario;
import com.vida.apirest.security.AppUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTokenVersionTest {

    private JwtUtil jwtUtil;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("dev-only-change-me-before-production-min-32-chars");
        props.setExpirationHours(24);
        jwtUtil = new JwtUtil(props);

        usuario = new Usuario();
        usuario.setUsuario("cajero");
        usuario.setTokenVersion(0);
    }

    @Test
    void tokenValidoSiVersionCoincide() {
        String token = jwtUtil.generateToken(usuario, List.of(), List.of(), "EMPRESA-A");
        assertTrue(jwtUtil.isTokenValid(token, details(), "EMPRESA-A"));
    }

    @Test
    void tokenInvalidoTrasInvalidarSesiones() {
        String token = jwtUtil.generateToken(usuario, List.of(), List.of(), "EMPRESA-A");
        usuario.invalidarTokens();
        assertFalse(jwtUtil.isTokenValid(token, details(), "EMPRESA-A"));
    }

    @Test
    void tokenNuevoValidoTrasInvalidarSesiones() {
        usuario.invalidarTokens();
        String token = jwtUtil.generateToken(usuario, List.of(), List.of(), "EMPRESA-A");
        assertTrue(jwtUtil.isTokenValid(token, details(), "EMPRESA-A"));
    }

    private UserDetails details() {
        return new AppUserDetails(usuario, List.of());
    }
}
