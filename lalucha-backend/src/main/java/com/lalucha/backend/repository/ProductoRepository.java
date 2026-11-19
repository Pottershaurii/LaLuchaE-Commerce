package com.lalucha.backend.repository;

import com.lalucha.backend.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findByActivoTrueOrderByIdDesc();

    List<Producto> findByCategoriaIdAndActivoTrue(Long categoriaId);

    long countByCategoriaId(Long categoriaId);

    boolean existsBySku(String sku);

    boolean existsBySlug(String slug);
}
