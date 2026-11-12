package com.lalucha.backend.service;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas unitarias de JwtService (sin levantar Spring ni la base de datos).
 * Cubren la generacion del token y los casos que deben rechazarse.
 */
class JwtServiceTest {

    private static final String CLAVE = "clave-de-prueba-unitaria-con-mas-de-32-caracteres";
    private static final long UNA_HORA = 3_600_000L;

    private final JwtService jwtService = new JwtService(CLAVE, UNA_HORA);

    @Test
    @DisplayName("Rechaza una clave de menos de 32 caracteres")
    void rechazaClaveCorta() {
        assertThatThrownBy(() -> new JwtService("corta", UNA_HORA))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }

    @Test
    @DisplayName("Genera un token con el correo, el rol y el nombre")
    void generaTokenConClaims() {
        String token = jwtService.generateToken("juan@example.com", "ROLE_CLIENTE", "Juan Perez");

        assertThat(jwtService.extractEmail(token)).isEqualTo("juan@example.com");
        assertThat(jwtService.extractRol(token)).isEqualTo("ROLE_CLIENTE");
        Optional<Claims> claims = jwtService.validarToken(token);
        assertThat(claims).isPresent();
        assertThat(claims.get().get("nombre")).isEqualTo("Juan Perez");
        assertThat(claims.get().getExpiration()).isAfter(claims.get().getIssuedAt());
    }

    @Test
    @DisplayName("Rechaza un token firmado con otra clave")
    void rechazaTokenDeOtraClave() {
        JwtService atacante = new JwtService("otra-clave-distinta-con-mas-de-32-caracteres", UNA_HORA);
        String tokenFalso = atacante.generateToken("juan@example.com", "ROLE_ADMIN", "Atacante");

        assertThat(jwtService.validarToken(tokenFalso)).isEmpty();
    }

    @Test
    @DisplayName("Rechaza un token cuyo rol fue editado")
    void rechazaTokenManipulado() {
        String token = jwtService.generateToken("juan@example.com", "ROLE_CLIENTE", "Juan Perez");
        String[] partes = token.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8)
                .replace("ROLE_CLIENTE", "ROLE_ADMIN");
        String payloadEditado = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));

        assertThat(jwtService.validarToken(partes[0] + "." + payloadEditado + "." + partes[2])).isEmpty();
    }

    @Test
    @DisplayName("Rechaza un token vencido")
    void rechazaTokenVencido() {
        JwtService vencido = new JwtService(CLAVE, -1_000L);
        String token = vencido.generateToken("juan@example.com", "ROLE_CLIENTE", "Juan Perez");

        assertThat(jwtService.validarToken(token)).isEmpty();
    }

    @Test
    @DisplayName("Rechaza tokens mal formados o vacios")
    void rechazaTokenMalFormado() {
        assertThat(jwtService.validarToken("abc.def.ghi")).isEmpty();
        assertThat(jwtService.validarToken("")).isEmpty();
    }
}
