package com.lalucha.backend.repository;

import com.lalucha.backend.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Método para buscar usuario por correo (útil para el login)
    Optional<Usuario> findByEmail(String email);

    // Método para verificar si un correo ya existe antes de registrarlo
    boolean existsByEmail(String email);
}