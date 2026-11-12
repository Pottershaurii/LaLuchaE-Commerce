package com.lalucha.backend.repository;

import com.lalucha.backend.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findByActivoTrueOrderByOrdenAsc();

    Optional<Categoria> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);
}