package com.lalucha.backend.service;

import com.lalucha.backend.model.Usuario;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.OffsetDateTime;

/**
 * Genera y valida el codigo de verificacion de correo.
 * El codigo y su vencimiento se guardan en la BD
 * (usuarios.token_verificacion / usuarios.token_expira_en),
 * asi no se pierden si el servidor se reinicia.
 */
@Service
public class VerificationService {

    /** Minutos de validez del codigo enviado por correo. */
    public static final long MINUTOS_VALIDEZ = 15;

    private final SecureRandom random = new SecureRandom();

    /** Genera un codigo de 6 digitos, lo asigna al usuario con su vencimiento y lo devuelve. */
    public String asignarCodigo(Usuario usuario) {
        String codigo = String.valueOf(100000 + random.nextInt(900000));
        usuario.setTokenVerificacion(codigo);
        usuario.setTokenExpiraEn(OffsetDateTime.now().plusMinutes(MINUTOS_VALIDEZ));
        return codigo;
    }

    public boolean codigoCoincide(Usuario usuario, String codigo) {
        return codigo != null
                && usuario.getTokenVerificacion() != null
                && usuario.getTokenVerificacion().equals(codigo.trim());
    }

    public boolean codigoVencido(Usuario usuario) {
        return usuario.getTokenExpiraEn() == null
                || usuario.getTokenExpiraEn().isBefore(OffsetDateTime.now());
    }

    /** Marca el correo como verificado y limpia el codigo. */
    public void marcarVerificado(Usuario usuario) {
        usuario.setEmailVerificado(true);
        usuario.setTokenVerificacion(null);
        usuario.setTokenExpiraEn(null);
    }
}
