package com.lalucha.backend.controller;

import com.lalucha.backend.dto.UsuarioResumen;
import com.lalucha.backend.repository.UsuarioRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Rutas de administracion: solo rol ADMIN (ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UsuarioRepository usuarioRepository;

    public AdminController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /** Lista de usuarios para el panel de administracion (sin datos sensibles). */
    @GetMapping("/usuarios")
    public List<UsuarioResumen> usuarios() {
        return usuarioRepository.findAll().stream()
                .map(UsuarioResumen::de)
                .toList();
    }
}
