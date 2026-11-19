package com.lalucha.backend.controller;

import com.lalucha.backend.model.Producto;
import com.lalucha.backend.service.ProductoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Panel de productos (HU-10).
 * - GET    /api/productos        catalogo publico (solo productos activos)
 * - POST   /api/productos        crear (ADMIN)
 * - PUT    /api/productos/{id}   editar (ADMIN)
 * - DELETE /api/productos/{id}   desactivar, no borra el registro (ADMIN)
 */
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
            Producto creado = service.crear(
                    texto(body, "nombre"),
                    texto(body, "descripcion"),
                    decimal(body, "precio"),
                    texto(body, "imagenUrl"),
                    entero(body, "categoriaId"),
                    entero(body, "stockMinimo") == null ? null : entero(body, "stockMinimo").intValue());
            return ResponseEntity.ok(creado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            Object activo = body.get("activo");
            Producto actualizado = service.actualizar(id,
                    texto(body, "nombre"),
                    texto(body, "descripcion"),
                    decimal(body, "precio"),
                    texto(body, "imagenUrl"),
                    entero(body, "categoriaId"),
                    entero(body, "stockMinimo") == null ? null : entero(body, "stockMinimo").intValue(),
                    activo == null ? null : Boolean.valueOf(activo.toString()));
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

    // Lectura tolerante del JSON: un campo vacio cuenta como ausente y un numero mal escrito
    // termina en 400 (NumberFormatException es una IllegalArgumentException), no en 500.

    private static String texto(Map<String, Object> body, String campo) {
        Object valor = body.get(campo);
        return valor == null ? null : valor.toString();
    }

    private static BigDecimal decimal(Map<String, Object> body, String campo) {
        String valor = texto(body, campo);
        if (valor == null || valor.isBlank()) return null;
        try {
            return new BigDecimal(valor.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El campo " + campo + " debe ser un número.");
        }
    }

    private static Long entero(Map<String, Object> body, String campo) {
        String valor = texto(body, campo);
        if (valor == null || valor.isBlank()) return null;
        try {
            return Long.valueOf(valor.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El campo " + campo + " debe ser un número entero.");
        }
    }
}
