package com.vida.apirest.utils;

import com.vida.apirest.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;


@Component
public class JwtUtil {
    private final SecretKey key = Keys.hmacShaKeyFor(
            "7017980b1c5ea75a3c58e7faabd1324ba6ab068ef972ed18f361c5346e3a3d0a".getBytes(StandardCharsets.UTF_8)
    );

    private Claims getClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();

    }

    public String generatToken(Usuario usuario) {

        long expirationMillis = 1000 * 60 * 60 * 24; //24 hora
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMillis);
        return Jwts.builder().subject(usuario.getEmail()).issuedAt(now).expiration(expiry).signWith(key).compact();
    }

    public String extractNombreDeUsuario(String token) {
        return getClaims(token).getSubject();
    }

    private boolean isTokenExpired(String token) {

        return getClaims(token).getExpiration().before(new Date());

    }

    public boolean isTokenValid (String token, UserDetails userDetails){
        String usuario = extractNombreDeUsuario(token);
        return  usuario.equals(userDetails.getUsername()) && !isTokenExpired(token);

    }

}
