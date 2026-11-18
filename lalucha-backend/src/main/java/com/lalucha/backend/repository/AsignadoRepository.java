package com.lalucha.backend.repository;

import com.lalucha.backend.model.Asignado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AsignadoRepository extends JpaRepository<Asignado, Short> {
}
