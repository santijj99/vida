package com.vida.apirest.config;

import com.vida.apirest.model.auth.Usuario;
import com.vida.apirest.security.AppUserDetails;
import com.vida.apirest.servicies.CustomUserDetailService;
import com.vida.apirest.tenant.TenantContext;
import com.vida.apirest.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private JwtUtil jwtUtil;
    private CustomUserDetailService userDetailsService;
    private JwtAuthenticationFilter filter;
    private FilterChain chain;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("dev-only-change-me-before-production-min-32-chars");
        props.setExpirationHours(24);
        jwtUtil = new JwtUtil(props);
        userDetailsService = mock(CustomUserDetailService.class);
        filter = new JwtAuthenticationFilter(jwtUtil, userDetailsService);
        chain = mock(FilterChain.class);

        usuario = new Usuario();
        usuario.setUsuario("cajero");
        usuario.setActivo(true);
        usuario.setTokenVersion(0);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void bearerBasuraNoTiraYSigueSinAutenticar() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/venta");
        request.addHeader("Authorization", "Bearer esto-no-es-un-jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(userDetailsService, never()).loadUserByUsername(any());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void tokenValidoDelTenantAutentica() throws Exception {
        TenantContext.setCodigoLicencia("EMPRESA-A");
        when(userDetailsService.loadUserByUsername("cajero"))
                .thenReturn(new AppUserDetails(usuario, List.of()));
        MockHttpServletRequest request = bearerRequest(token("EMPRESA-A"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("cajero", SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Test
    void tokenDeOtroTenantNoAutentica() throws Exception {
        TenantContext.setCodigoLicencia("EMPRESA-B");
        when(userDetailsService.loadUserByUsername("cajero"))
                .thenReturn(new AppUserDetails(usuario, List.of()));
        MockHttpServletRequest request = bearerRequest(token("EMPRESA-A"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void tokenConVersionViejaNoAutentica() throws Exception {
        TenantContext.setCodigoLicencia("EMPRESA-A");
        String token = token("EMPRESA-A");
        usuario.invalidarTokens();
        when(userDetailsService.loadUserByUsername("cajero"))
                .thenReturn(new AppUserDetails(usuario, List.of()));
        MockHttpServletRequest request = bearerRequest(token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    @Test
    void usuarioInactivoNoAutentica() throws Exception {
        TenantContext.setCodigoLicencia("EMPRESA-A");
        when(userDetailsService.loadUserByUsername("cajero"))
                .thenThrow(new UsernameNotFoundException("Usuario inactivo"));
        MockHttpServletRequest request = bearerRequest(token("EMPRESA-A"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    private String token(String licencia) {
        return jwtUtil.generateToken(usuario, List.of(), List.of(), licencia);
    }

    private static MockHttpServletRequest bearerRequest(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/venta");
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }
}
