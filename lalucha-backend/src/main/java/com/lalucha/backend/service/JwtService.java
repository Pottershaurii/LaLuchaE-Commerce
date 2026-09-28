package com.lalucha.backend.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    // Clave secreta de 256 bits para firmar los tokens JWT
    private static final String SECRET_KEY_STRING = "LaLuchaSangucheriaCriollaSecretKeyForJwtAuthentication2026";
    private final Key key = Keys.hmacShaKeyFor(SECRET_KEY_STRING.getBytes());

    // Tiempo de expiración: 24 horas (en milisegundos)
    private static final long EXPIRATION_TIME = 86400000;

    public String generateToken(String email, String rol, String nombre) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("rol", rol);
        claims.put("nombre", nombre);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    public String extractRol(String token) {
        return (String) extractClaims(token).get("rol");
    }

    private Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}