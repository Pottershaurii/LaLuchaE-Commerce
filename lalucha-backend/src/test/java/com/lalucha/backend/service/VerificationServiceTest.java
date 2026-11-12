package com.lalucha.backend.service;

import com.lalucha.backend.model.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** Pruebas unitarias del codigo de verificacion de correo. */
class VerificationServiceTest {

    private final VerificationService servicio = new VerificationService();

    @Test
    @DisplayName("Asigna un codigo de 6 digitos que vence en 15 minutos")
    void asignaCodigo() {
        Usuario usuario = new Usuario();

        String codigo = servicio.asignarCodigo(usuario);

        assertThat(codigo).matches("\\d{6}");
        assertThat(usuario.getTokenVerificacion()).isEqualTo(codigo);
        assertThat(usuario.getTokenExpiraEn())
                .isAfter(OffsetDateTime.now().plusMinutes(VerificationService.MINUTOS_VALIDEZ - 1))
                .isBefore(OffsetDateTime.now().plusMinutes(VerificationService.MINUTOS_VALIDEZ + 1));
    }

    @Test
    @DisplayName("Compara el codigo ignorando espacios y rechaza valores distintos o nulos")
    void comparaCodigo() {
        Usuario usuario = new Usuario();
        usuario.setTokenVerificacion("123456");

        assertThat(servicio.codigoCoincide(usuario, " 123456 ")).isTrue();
        assertThat(servicio.codigoCoincide(usuario, "654321")).isFalse();
        assertThat(servicio.codigoCoincide(usuario, null)).isFalse();

        usuario.setTokenVerificacion(null);
        assertThat(servicio.codigoCoincide(usuario, "123456")).isFalse();
    }

    @Test
    @DisplayName("Detecta un codigo vencido o sin fecha de vencimiento")
    void detectaVencimiento() {
        Usuario usuario = new Usuario();

        usuario.setTokenExpiraEn(OffsetDateTime.now().plusMinutes(5));
        assertThat(servicio.codigoVencido(usuario)).isFalse();

        usuario.setTokenExpiraEn(OffsetDateTime.now().minusMinutes(1));
        assertThat(servicio.codigoVencido(usuario)).isTrue();

        usuario.setTokenExpiraEn(null);
        assertThat(servicio.codigoVencido(usuario)).isTrue();
    }

    @Test
    @DisplayName("Al verificar, marca el correo y limpia el codigo")
    void marcaVerificado() {
        Usuario usuario = new Usuario();
        servicio.asignarCodigo(usuario);

        servicio.marcarVerificado(usuario);

        assertThat(usuario.isEmailVerificado()).isTrue();
        assertThat(usuario.getTokenVerificacion()).isNull();
        assertThat(usuario.getTokenExpiraEn()).isNull();
    }
}
