package com.lalucha.backend.repository;

import com.lalucha.backend.model.Prioridad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PrioridadRepository extends JpaRepository<Prioridad, Short> {

    Optional<Prioridad> findByCodigo(String codigo);
}
