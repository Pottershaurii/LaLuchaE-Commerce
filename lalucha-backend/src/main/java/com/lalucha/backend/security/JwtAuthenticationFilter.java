package com.lalucha.backend.security;

import com.lalucha.backend.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * OWASP A01 - Broken Access Control.
 * Lee el header "Authorization: Bearer <token>", valida firma y vencimiento
 * del JWT y registra al usuario autenticado con su rol (ROLE_CLIENTE / ROLE_ADMIN).
 * Si el token falta o es invalido, la peticion sigue como anonima y
 * Spring Security responde 401 en las rutas protegidas.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Atributo que usa el entry point para explicar por que se rechazo el token. */
    public static final String ATTR_TOKEN_INVALIDO = "lalucha.tokenInvalido";

    private static final String PREFIJO = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith(PREFIJO)) {
            String token = header.substring(PREFIJO.length()).trim();
            Optional<Claims> claims = jwtService.validarToken(token);

            if (claims.isPresent() && claims.get().getSubject() != null
                    && claims.get().get("rol") instanceof String rol) {
                UsernamePasswordAuthenticationToken autenticacion =
                        new UsernamePasswordAuthenticationToken(
                                claims.get().getSubject(),
                                null,
                                List.of(new SimpleGrantedAuthority(rol)));
                SecurityContextHolder.getContext().setAuthentication(autenticacion);
            } else {
                SecurityContextHolder.clearContext();
                request.setAttribute(ATTR_TOKEN_INVALIDO, Boolean.TRUE);
            }
        }

        filterChain.doFilter(request, response);
    }
}
