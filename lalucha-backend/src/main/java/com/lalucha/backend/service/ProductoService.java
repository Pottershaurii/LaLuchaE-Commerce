package com.lalucha.backend.service;

import com.lalucha.backend.model.Categoria;
import com.lalucha.backend.model.Producto;
import com.lalucha.backend.repository.CategoriaRepository;
import com.lalucha.backend.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProductoService {

    private final ProductoRepository productoRepo;
    private final CategoriaRepository categoriaRepo;

    public ProductoService(ProductoRepository productoRepo, CategoriaRepository categoriaRepo) {
        this.productoRepo = productoRepo;
        this.categoriaRepo = categoriaRepo;
    }

    public List<Producto> listarActivos() {
        return productoRepo.findByActivoTrueOrderByIdDesc();
    }

    @Transactional
    public Producto crear(String nombre, String descripcion, BigDecimal precio,
                          String imagenUrl, Long categoriaId, Integer stockMinimo) {
        Categoria categoria = categoriaRepo.findById(categoriaId)
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));

        Producto p = new Producto();
        p.setCategoria(categoria);
        p.setNombre(nombre);
        p.setDescripcion(descripcion);
        p.setPrecio(precio);
        p.setImagenUrl(imagenUrl);
        p.setStockMinimo(stockMinimo != null ? stockMinimo : 5);
        p.setSku(generarSku(nombre, categoria));
        p.setSlug(generarSlug(nombre));
        p.setActivo(true);
        return productoRepo.save(p);
    }

    @Transactional
    public Producto actualizar(Long id, String nombre, String descripcion, BigDecimal precio,
                               String imagenUrl, Long categoriaId, Integer stockMinimo, Boolean activo) {
        Producto p = productoRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));

        if (nombre != null) p.setNombre(nombre);
        if (descripcion != null) p.setDescripcion(descripcion);
        if (precio != null) p.setPrecio(precio);
        if (imagenUrl != null) p.setImagenUrl(imagenUrl);
        if (stockMinimo != null) p.setStockMinimo(stockMinimo);
        if (activo != null) p.setActivo(activo);
        if (categoriaId != null) {
            Categoria categoria = categoriaRepo.findById(categoriaId)
                    .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));
            p.setCategoria(categoria);
        }
        return productoRepo.save(p);
    }

    @Transactional
    public void desactivar(Long id) {
        Producto p = productoRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        p.setActivo(false);
        productoRepo.save(p);
    }

    private String generarSku(String nombre, Categoria categoria) {
        String prefijo = categoria.getCodigo().substring(0, 2).toUpperCase();
        String base = nombre.toUpperCase()
                .replaceAll("[ÁÉÍÓÚ]", "A")
                .replaceAll("[^A-Z0-9]", "");
        base = base.length() >= 3 ? base.substring(0, 3) : base;
        long siguiente = productoRepo.countByCategoriaId(categoria.getId()) + 1;
        return prefijo + "-" + base + "-" + String.format("%02d", siguiente);
    }

    private String generarSlug(String nombre) {
        return nombre.toLowerCase()
                .replaceAll("[áéíóú]", "a")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}