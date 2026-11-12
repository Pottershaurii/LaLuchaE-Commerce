package com.lalucha.backend.service;

import com.lalucha.backend.model.Categoria;
import com.lalucha.backend.repository.CategoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository repo;

    public CategoriaService(CategoriaRepository repo) {
        this.repo = repo;
    }

    public List<Categoria> listarActivas() {
        return repo.findByActivoTrueOrderByOrdenAsc();
    }

    @Transactional
    public Categoria crear(String codigo, String nombre, String descripcion, String icono, Integer orden) {
        if (repo.existsByCodigo(codigo)) {
            throw new IllegalArgumentException("Ya existe una categoría con el código " + codigo);
        }
        Categoria c = new Categoria();
        c.setCodigo(codigo);
        c.setNombre(nombre);
        c.setDescripcion(descripcion);
        c.setIcono(icono);
        c.setOrden(orden != null ? orden : 0);
        c.setActivo(true);
        return repo.save(c);
    }
}