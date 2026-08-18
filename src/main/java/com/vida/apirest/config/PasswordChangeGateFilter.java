package com.vida.apirest.config;

import com.vida.apirest.security.AppUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Si el usuario tiene password temporal de bootstrap, solo puede ver /auth/me
 * y cambiar la contraseña. El resto de la API responde 403.
 */
@Component
public class PasswordChangeGateFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null
                && auth.getPrincipal() instanceof AppUserDetails details
                && details.getUsuario().debeCambiarPassword()
                && !rutaPermitida(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"message\":\"Debés cambiar la contraseña temporal antes de continuar\",\"statusCode\":403}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean rutaPermitida(HttpServletRequest request) {
        String path = servletPath(request);
        String method = request.getMethod() == null ? "" : request.getMethod().toUpperCase();
        if ("GET".equals(method) && "/auth/me".equals(path)) {
            return true;
        }
        return "POST".equals(method) && "/auth/cambiar-password-inicial".equals(path);
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
}
