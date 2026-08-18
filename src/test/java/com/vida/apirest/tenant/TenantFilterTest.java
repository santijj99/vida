package com.vida.apirest.tenant;

import com.vida.apirest.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantFilterTest {

    private TenantDataSourceManager tenantDataSourceManager;
    private JwtUtil jwtUtil;
    private TenantFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        tenantDataSourceManager = mock(TenantDataSourceManager.class);
        jwtUtil = mock(JwtUtil.class);
        filter = new TenantFilter(tenantDataSourceManager, jwtUtil);
        chain = mock(FilterChain.class);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void sinMultiTenantNoTocaElPool() throws Exception {
        when(tenantDataSourceManager.isMultiTenantEnabled()).thenReturn(false);
        MockHttpServletRequest request = request("GET", "/venta");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(tenantDataSourceManager, never()).ensureTenantReady(anyString());
        assertNull(TenantContext.getCodigoLicencia());
    }

    @Test
    void actuatorPasaSinLicencia() throws Exception {
        when(tenantDataSourceManager.isMultiTenantEnabled()).thenReturn(true);
        MockHttpServletRequest request = request("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(tenantDataSourceManager, never()).ensureTenantReady(anyString());
    }

    @Test
    void loginSinHeaderEs403() throws Exception {
        when(tenantDataSourceManager.isMultiTenantEnabled()).thenReturn(true);
        MockHttpServletRequest request = request("POST", "/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("X-Licencia-Codigo"));
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void bearerConClaimDistintoAlHeaderEs403() throws Exception {
        when(tenantDataSourceManager.isMultiTenantEnabled()).thenReturn(true);
        when(jwtUtil.extractLicencia("tok")).thenReturn("EMPRESA-A");
        MockHttpServletRequest request = request("GET", "/venta");
        request.addHeader("Authorization", "Bearer tok");
        request.addHeader(TenantFilter.HEADER, "EMPRESA-B");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("no coincide"));
        verify(tenantDataSourceManager, never()).ensureTenantReady(anyString());
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void requestAutenticadoUsaClaimYPreparaTenant() throws Exception {
        when(tenantDataSourceManager.isMultiTenantEnabled()).thenReturn(true);
        when(jwtUtil.extractLicencia("tok")).thenReturn("EMPRESA-A");
        MockHttpServletRequest request = request("GET", "/venta");
        request.addHeader("Authorization", "Bearer tok");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(tenantDataSourceManager).ensureTenantReady("EMPRESA-A");
        verify(chain).doFilter(request, response);
        assertEquals(200, response.getStatus());
        assertNull(TenantContext.getCodigoLicencia());
    }

    @Test
    void licenciaInvalidaNoSigueLaCadena() throws Exception {
        when(tenantDataSourceManager.isMultiTenantEnabled()).thenReturn(true);
        when(jwtUtil.extractLicencia("tok")).thenReturn("EMPRESA-A");
        doThrow(new RuntimeException("Licencia revocada"))
                .when(tenantDataSourceManager).ensureTenantReady("EMPRESA-A");
        MockHttpServletRequest request = request("GET", "/venta");
        request.addHeader("Authorization", "Bearer tok");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("Licencia revocada"));
        verify(chain, never()).doFilter(request, response);
    }

    private static MockHttpServletRequest request(String method, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRequestURI(uri);
        return request;
    }
}
