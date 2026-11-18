package com.lalucha.backend.service;

import com.lalucha.backend.model.Categoria;
import com.lalucha.backend.model.Producto;
import com.lalucha.backend.repository.CategoriaRepository;
import com.lalucha.backend.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

@Service
public class ProductoService {

    private static final int NOMBRE_MAX = 120;
    private static final int IMAGEN_MAX = 500;

    private final ProductoRepository productoRepo;
    private final CategoriaRepository categoriaRepo;

    public ProductoService(ProductoRepository productoRepo, CategoriaRepository categoriaRepo) {
        this.productoRepo = productoRepo;
        this.categoriaRepo = categoriaRepo;
    }

    @Transactional(readOnly = true)
    public List<Producto> listarActivos() {
        return productoRepo.findByActivoTrueOrderByIdDesc();
    }

    @Transactional
    public Producto crear(String nombre, String descripcion, BigDecimal precio,
                          String imagenUrl, Long categoriaId, Integer stockMinimo) {
        validarNombre(nombre);
        validarPrecio(precio);
        validarImagen(imagenUrl);
        validarStock(stockMinimo);
        if (categoriaId == null) {
            throw new IllegalArgumentException("La categoría es obligatoria.");
        }
        Categoria categoria = buscarCategoria(categoriaId);

        Producto p = new Producto();
        p.setCategoria(categoria);
        p.setNombre(nombre.trim());
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

        if (nombre != null) {
            validarNombre(nombre);
            p.setNombre(nombre.trim());
        }
        if (descripcion != null) p.setDescripcion(descripcion);
        if (precio != null) {
            validarPrecio(precio);
            p.setPrecio(precio);
        }
        if (imagenUrl != null) {
            validarImagen(imagenUrl);
            p.setImagenUrl(imagenUrl);
        }
        if (stockMinimo != null) {
            validarStock(stockMinimo);
            p.setStockMinimo(stockMinimo);
        }
        if (activo != null) p.setActivo(activo);
        if (categoriaId != null) p.setCategoria(buscarCategoria(categoriaId));
        return productoRepo.save(p);
    }

    @Transactional
    public void desactivar(Long id) {
        Producto p = productoRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        p.setActivo(false);
        productoRepo.save(p);
    }

    // ------------------------------------------------------------ validaciones
    // Se validan aqui para responder 400 con un mensaje claro en vez de un 500 de la base de datos.

    private void validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio.");
        }
        if (nombre.trim().length() > NOMBRE_MAX) {
            throw new IllegalArgumentException("El nombre admite hasta " + NOMBRE_MAX + " caracteres.");
        }
    }

    private void validarPrecio(BigDecimal precio) {
        if (precio == null) {
            throw new IllegalArgumentException("El precio es obligatorio.");
        }
        if (precio.signum() < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo.");
        }
    }

    private void validarImagen(String imagenUrl) {
        if (imagenUrl != null && imagenUrl.length() > IMAGEN_MAX) {
            throw new IllegalArgumentException("La URL de la imagen admite hasta " + IMAGEN_MAX + " caracteres.");
        }
    }

    private void validarStock(Integer stockMinimo) {
        if (stockMinimo != null && stockMinimo < 0) {
            throw new IllegalArgumentException("El stock mínimo no puede ser negativo.");
        }
    }

    private Categoria buscarCategoria(Long categoriaId) {
        return categoriaRepo.findById(categoriaId)
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));
    }

    // ------------------------------------------------------------ SKU y slug

    /** SKU secuencial por categoria (ej. SA-BUT-05). Si ya existe, prueba el siguiente numero. */
    private String generarSku(String nombre, Categoria categoria) {
        String codigo = categoria.getCodigo().toUpperCase(Locale.ROOT);
        String prefijo = codigo.substring(0, Math.min(2, codigo.length()));
        String base = sinTildes(nombre).toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        if (base.isEmpty()) base = "PRD";
        base = base.substring(0, Math.min(3, base.length()));

        long siguiente = productoRepo.countByCategoriaId(categoria.getId()) + 1;
        String sku = prefijo + "-" + base + "-" + String.format("%02d", siguiente);
        while (productoRepo.existsBySku(sku)) {
            siguiente++;
            sku = prefijo + "-" + base + "-" + String.format("%02d", siguiente);
        }
        return sku;
    }

    /** Slug para URLs (ej. "Sánguche de Piña" -> "sanguche-de-pina"); unico, agrega -2, -3... si se repite. */
    private String generarSlug(String nombre) {
        String base = sinTildes(nombre).toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        if (base.isEmpty()) base = "producto";
        if (base.length() > 130) base = base.substring(0, 130);

        String slug = base;
        int n = 2;
        while (productoRepo.existsBySlug(slug)) {
            slug = base + "-" + n++;
        }
        return slug;
    }

    private static String sinTildes(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
