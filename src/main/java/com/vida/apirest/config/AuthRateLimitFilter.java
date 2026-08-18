package com.vida.apirest.config;

import com.vida.apirest.security.AuthRateLimiter;
import com.vida.apirest.tenant.TenantContext;
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
 * Rate limit por IP en login / forgot / reset / soporte (S-05, S-09).
 */
@Component
@RequiredArgsConstructor
public class AuthRateLimitFilter extends OncePerRequestFilter {

    static final String MSG = "Demasiados intentos. Probá de nuevo en unos minutos.";

    private static final int MAX_LOGIN_IP = 30;
    private static final int MAX_SOPORTE_IP = 10;
    private static final int MAX_FORGOT_IP = 8;
    private static final int MAX_RESET_IP = 20;

    private final AuthRateLimiter limiter;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = servletPath(request);
        return !"/auth/login".equals(path)
                && !"/auth/soporte".equals(path)
                && !"/auth/forgot-password".equals(path)
                && !"/auth/reset-password".equals(path);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = servletPath(request);
        int max = switch (path) {
            case "/auth/login" -> MAX_LOGIN_IP;
            case "/auth/soporte" -> MAX_SOPORTE_IP;
            case "/auth/forgot-password" -> MAX_FORGOT_IP;
            case "/auth/reset-password" -> MAX_RESET_IP;
            default -> 30;
        };
        String key = "ip:" + path + ":" + tenantPart() + ":" + clientIp(request);
        if (!limiter.tryConsume(key, max)) {
            writeTooMany(response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static String tenantPart() {
        String codigo = TenantContext.getCodigoLicencia();
        return codigo == null || codigo.isBlank() ? "-" : codigo;
    }

    public static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String addr = request.getRemoteAddr();
        return addr == null ? "unknown" : addr;
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

    private static void writeTooMany(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"message\":\"" + MSG + "\",\"statusCode\":429}");
    }
}
