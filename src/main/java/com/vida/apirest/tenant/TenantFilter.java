package com.vida.apirest.tenant;

import com.vida.apirest.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Resuelve el tenant desde header {@code X-Licencia-Codigo} o claim JWT {@code licencia}.
 * Registrado solo en SecurityFilterChain (ver {@code TenantFilterRegistration}).
 */
@Component
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Licencia-Codigo";
    public static final String HEADER_DEVICE_UUID = "X-Device-Uuid";
    public static final String HEADER_DEVICE_NOMBRE = "X-Device-Nombre";

    private final TenantDataSourceManager tenantDataSourceManager;
    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            TenantContext.setDeviceUuid(request.getHeader(HEADER_DEVICE_UUID));
            TenantContext.setDeviceNombre(request.getHeader(HEADER_DEVICE_NOMBRE));
            if (tenantDataSourceManager.isMultiTenantEnabled()) {
                String codigo = request.getHeader(HEADER);
                if (codigo == null || codigo.isBlank()) {
                    codigo = extractFromBearer(request);
                }
                if (codigo == null || codigo.isBlank()) {
                    if (!isExemptWithoutTenant(request)) {
                        writeForbidden(response, "Falta el código de licencia (header X-Licencia-Codigo)");
                        return;
                    }
                } else {
                    TenantContext.setCodigoLicencia(codigo);
                    if (!isExemptWithoutTenant(request)) {
                        try {
                            tenantDataSourceManager.ensureTenantReady(codigo);
                        } catch (RuntimeException ex) {
                            writeForbidden(response, ex.getMessage() != null
                                    ? ex.getMessage()
                                    : "No se pudo resolver la base de datos del tenant");
                            return;
                        }
                    }
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private String extractFromBearer(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return null;
        }
        try {
            return jwtUtil.extractLicencia(auth.substring(7));
        } catch (Exception e) {
            return null;
        }
    }

    /** Rutas que pueden pasar sin tenant (health / error). Auth siempre exige tenant en multi-tenant. */
    private boolean isExemptWithoutTenant(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null && (path.startsWith("/actuator") || path.equals("/error"));
    }

    private void writeForbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String safe = message == null ? "Acceso denegado" : message.replace("\"", "'");
        response.getWriter().write("{\"message\":\"" + safe + "\"}");
    }
}
