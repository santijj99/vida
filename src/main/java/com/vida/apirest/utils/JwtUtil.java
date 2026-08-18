package com.vida.apirest.utils;

import com.vida.apirest.config.JwtProperties;
import com.vida.apirest.model.auth.Usuario;
import com.vida.apirest.security.AppUserDetails;
import com.vida.apirest.tenant.TenantContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Component
public class JwtUtil {

    private final JwtProperties jwtProperties;
    private final SecretKey key;

    public JwtUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    private Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public String generateToken(Usuario usuario, Collection<String> roles, Collection<String> permissions) {
        return generateToken(usuario, roles, permissions, null);
    }

    public String generateToken(
            Usuario usuario,
            Collection<String> roles,
            Collection<String> permissions,
            String codigoLicencia
    ) {
        return generateToken(usuario, roles, permissions, codigoLicencia, null);
    }

    public String generateToken(
            Usuario usuario,
            Collection<String> roles,
            Collection<String> permissions,
            String codigoLicencia,
            java.time.Instant expiresAt
    ) {
        long expirationMillis = jwtProperties.getExpirationHours() * 60 * 60 * 1000;
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMillis);
        if (expiresAt != null) {
            Date supportExpiry = Date.from(expiresAt);
            if (supportExpiry.before(expiry)) {
                expiry = supportExpiry;
            }
        }
        var builder = Jwts.builder()
                .subject(usuario.getUsuario())
                .claim("roles", roles)
                .claim("permissions", permissions)
                .claim("ver", usuario.tokenVersionOrZero())
                .issuedAt(now)
                .expiration(expiry);
        if (codigoLicencia != null && !codigoLicencia.isBlank()) {
            builder.claim("licencia", codigoLicencia.trim());
        }
        return builder.signWith(key).compact();
    }

    /** Compatibilidad con llamadas existentes. */
    public String generatToken(Usuario usuario) {
        return generateToken(usuario, List.of(), List.of(), null);
    }

    public String extractNombreDeUsuario(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractLicencia(String token) {
        Object raw = parseClaims(token).get("licencia");
        return raw == null ? null : String.valueOf(raw);
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Object raw = parseClaims(token).get("roles");
        if (raw instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    public List<String> extractPermissions(String token) {
        Object raw = parseClaims(token).get("permissions");
        if (raw instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        return isTokenValid(token, userDetails, TenantContext.getCodigoLicencia());
    }

    /**
     * Además de subject y expiración:
     * <ul>
     *   <li>el claim {@code ver} debe coincidir con {@code Usuario.tokenVersion} (sesiones revocadas);</li>
     *   <li>si hay tenant esperado, el claim {@code licencia} debe coincidir.</li>
     * </ul>
     */
    public boolean isTokenValid(String token, UserDetails userDetails, String tenantEsperado) {
        Claims claims = parseClaims(token);
        String usuario = claims.getSubject();
        if (!usuario.equals(userDetails.getUsername()) || claims.getExpiration().before(new Date())) {
            return false;
        }
        if (extractTokenVersion(claims) != tokenVersionFrom(userDetails)) {
            return false;
        }
        if (tenantEsperado == null || tenantEsperado.isBlank()) {
            return true;
        }
        Object raw = claims.get("licencia");
        String claim = raw == null ? null : String.valueOf(raw).trim();
        return claim != null && !claim.isEmpty() && tenantEsperado.equals(claim);
    }

    /** Tokens viejos sin claim {@code ver} se tratan como versión 0. */
    private static int extractTokenVersion(Claims claims) {
        Object raw = claims.get("ver");
        if (raw instanceof Number n) {
            return n.intValue();
        }
        return 0;
    }

    private static int tokenVersionFrom(UserDetails userDetails) {
        if (userDetails instanceof AppUserDetails app) {
            return app.getUsuario().tokenVersionOrZero();
        }
        return 0;
    }
}
