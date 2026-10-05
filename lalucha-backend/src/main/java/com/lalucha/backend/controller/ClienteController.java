package com.lalucha.backend.controller;

import com.lalucha.backend.dto.UsuarioResumen;
import com.lalucha.backend.repository.UsuarioRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rutas del cliente autenticado (rol CLIENTE o ADMIN, ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/cliente")
public class ClienteController {

    private final UsuarioRepository usuarioRepository;

    public ClienteController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /** Perfil del usuario dueno del token: cada uno solo ve sus propios datos. */
    @GetMapping("/perfil")
    public ResponseEntity<UsuarioResumen> perfil(Authentication autenticacion) {
        return usuarioRepository.findByEmail(autenticacion.getName())
                .map(UsuarioResumen::de)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
