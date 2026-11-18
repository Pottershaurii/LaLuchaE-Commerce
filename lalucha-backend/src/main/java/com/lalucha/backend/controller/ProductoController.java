package com.lalucha.backend.controller;

import com.lalucha.backend.model.Producto;
import com.lalucha.backend.service.ProductoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService service;

    public ProductoController(ProductoService service) {
        this.service = service;
    }

    @GetMapping
    public List<Producto> listar() {
        return service.listarActivos();
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Map<String, Object> body) {
        try {
            String nombre = (String) body.get("nombre");
            String descripcion = (String) body.get("descripcion");
            BigDecimal precio = new BigDecimal(body.get("precio").toString());
            String imagenUrl = (String) body.get("imagenUrl");
            Long categoriaId = Long.valueOf(body.get("categoriaId").toString());
            Integer stockMinimo = body.get("stockMinimo") != null
                    ? Integer.valueOf(body.get("stockMinimo").toString()) : null;

            Producto creado = service.crear(nombre, descripcion, precio, imagenUrl, categoriaId, stockMinimo);
            return ResponseEntity.ok(creado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            String nombre = (String) body.get("nombre");
            String descripcion = (String) body.get("descripcion");
            BigDecimal precio = body.get("precio") != null ? new BigDecimal(body.get("precio").toString()) : null;
            String imagenUrl = (String) body.get("imagenUrl");
            Long categoriaId = body.get("categoriaId") != null ? Long.valueOf(body.get("categoriaId").toString()) : null;
            Integer stockMinimo = body.get("stockMinimo") != null ? Integer.valueOf(body.get("stockMinimo").toString()) : null;
            Boolean activo = body.get("activo") != null ? Boolean.valueOf(body.get("activo").toString()) : null;

            Producto actualizado = service.actualizar(id, nombre, descripcion, precio, imagenUrl, categoriaId, stockMinimo, activo);
            return ResponseEntity.ok(actualizado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> desactivar(@PathVariable Long id) {
        try {
            service.desactivar(id);
            return ResponseEntity.ok(Map.of("mensaje", "Producto desactivado"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}