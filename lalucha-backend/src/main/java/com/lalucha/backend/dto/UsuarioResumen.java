package com.lalucha.backend.dto;

import com.lalucha.backend.model.Usuario;

/**
 * Datos publicos de un usuario. Nunca incluye password_hash ni el codigo de verificacion.
 */
public record UsuarioResumen(
        Long id,
        String email,
        String nombres,
        String apellidos,
        String rol,
        boolean emailVerificado,
        boolean activo) {

    public static UsuarioResumen de(Usuario usuario) {
        return new UsuarioResumen(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getNombres(),
                usuario.getApellidos(),
                usuario.getRol().getCodigo(),
                usuario.isEmailVerificado(),
                usuario.isActivo());
    }
}
