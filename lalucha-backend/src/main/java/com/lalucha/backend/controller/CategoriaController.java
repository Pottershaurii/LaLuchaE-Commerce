package com.lalucha.backend.controller;

import com.lalucha.backend.model.Categoria;
import com.lalucha.backend.service.CategoriaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CategoriaService service;

    public CategoriaController(CategoriaService service) {
        this.service = service;
    }

    @GetMapping
    public List<Categoria> listar() {
        return service.listarActivas();
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Map<String, Object> body) {
        try {
            String codigo = (String) body.get("codigo");
            String nombre = (String) body.get("nombre");
            String descripcion = (String) body.get("descripcion");
            String icono = (String) body.get("icono");
            Integer orden = body.get("orden") != null ? ((Number) body.get("orden")).intValue() : 0;

            Categoria creada = service.crear(codigo, nombre, descripcion, icono, orden);
            return ResponseEntity.ok(creada);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}