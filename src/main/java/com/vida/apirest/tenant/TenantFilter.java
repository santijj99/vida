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
 * Con sesión autenticada el claim manda: el header no puede cambiar de empresa.
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
                if (isExemptWithoutTenant(request)) {
                    filterChain.doFilter(request, response);
                    return;
                }
                String bearer = request.getHeader("Authorization");
                boolean hasBearer = bearer != null && bearer.startsWith("Bearer ");
                String claim = hasBearer ? extractFromBearer(bearer) : null;
                TenantRequestBinder.Resolution resolution = TenantRequestBinder.resolve(
                        request.getHeader(HEADER),
                        claim,
                        hasBearer,
                        isPublicAuth(request)
                );
                if (!resolution.ok()) {
                    writeTenantError(response, resolution.error());
                    return;
                }
                TenantContext.setCodigoLicencia(resolution.codigo());
                try {
                    tenantDataSourceManager.ensureTenantReady(resolution.codigo());
                } catch (RuntimeException ex) {
                    writeForbidden(response, ex.getMessage() != null
                            ? ex.getMessage()
                            : "No se pudo resolver la base de datos del tenant");
                    return;
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private String extractFromBearer(String authorization) {
        try {
            return jwtUtil.extractLicencia(authorization.substring(7));
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isPublicAuth(HttpServletRequest request) {
        String path = servletPath(request);
        return "/auth/login".equals(path)
                || "/auth/soporte".equals(path)
                || "/auth/forgot-password".equals(path)
                || "/auth/reset-password".equals(path)
                || "/auth/register".equals(path);
    }

    /** Rutas que pueden pasar sin tenant (health / error). */
    private boolean isExemptWithoutTenant(HttpServletRequest request) {
        String path = servletPath(request);
        return path.startsWith("/actuator") || path.equals("/error");
    }

    private static String servletPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) {
            return "";
        }
        String ctx = request.getContextPath();
        if (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx)) {
            return uri.substring(ctx.length());
        }
        return uri;
    }

    private void writeTenantError(HttpServletResponse response, TenantRequestBinder.Error error) throws IOException {
        if (error == TenantRequestBinder.Error.MISSING_CLAIM) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "La sesión no tiene licencia; volvé a iniciar sesión");
            return;
        }
        if (error == TenantRequestBinder.Error.MISMATCH) {
            writeForbidden(response, "El código de licencia no coincide con la sesión");
            return;
        }
        writeForbidden(response, "Falta el código de licencia (header X-Licencia-Codigo)");
    }

    private void writeForbidden(HttpServletResponse response, String message) throws IOException {
        writeError(response, HttpServletResponse.SC_FORBIDDEN, message);
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String safe = message == null ? "Acceso denegado" : message.replace("\"", "'");
        response.getWriter().write("{\"message\":\"" + safe + "\"}");
    }
}
