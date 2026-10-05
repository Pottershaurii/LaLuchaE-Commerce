package com.lalucha.backend.config;

import com.lalucha.backend.security.JwtAuthenticationFilter;
import com.lalucha.backend.service.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

/**
 * OWASP A01 - Broken Access Control.
 * - Sesion sin estado: cada peticion se autentica con el JWT del login.
 * - Denegar por defecto: todo lo que no es publico exige token valido.
 * - Reglas por rol: /api/admin/** solo ADMIN; /api/cliente/** y /api/checkout/** CLIENTE o ADMIN.
 * - Respuestas 401 (sin token / token invalido) y 403 (rol sin permiso) en JSON.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtService jwtService;

    public SecurityConfig(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // API sin cookies de sesion: se autentica con Bearer token
            .cors(cors -> cors.configure(http))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Rutas publicas
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/health").permitAll()
                // Rutas por rol
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/cliente/**").hasAnyRole("CLIENTE", "ADMIN")
                .requestMatchers("/api/checkout/**").hasAnyRole("CLIENTE", "ADMIN")
                // Todo lo demas: denegado salvo usuario autenticado
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, e) -> {
                    boolean tokenInvalido = request.getAttribute(JwtAuthenticationFilter.ATTR_TOKEN_INVALIDO) != null;
                    escribirError(response, HttpServletResponse.SC_UNAUTHORIZED,
                            tokenInvalido
                                    ? "Token invalido o expirado."
                                    : "Debes iniciar sesion para acceder a este recurso.");
                })
                .accessDeniedHandler((request, response, e) ->
                    escribirError(response, HttpServletResponse.SC_FORBIDDEN,
                            "No tienes permisos para acceder a este recurso."))
            )
            .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private static void escribirError(HttpServletResponse response, int status, String mensaje) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"success\":false,\"status\":" + status + ",\"message\":\"" + mensaje + "\"}");
    }
}
