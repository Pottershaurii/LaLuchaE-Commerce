# HU-10 — Modelo de base de datos (La Lucha)

Historia: *Implementación de BD: definir tablas de usuarios, clientes, inventario y otros.*

## Tablas y su historia

| Grupo | Tablas | Cubre |
|---|---|---|
| Seguridad | `roles`, `usuarios` | HU-01 JWT y roles Admin / Cliente / Staff |
| Clientes | `clientes` | HU-02 registro y perfil |
| Catálogo | `categorias`, `productos`, `combo_items` | HU-03 + prototipo Figma |
| Personalización | `grupos_opcion`, `opciones`, `producto_opcion` | HU-04 pan / salsas / adiciones |
| Inventario | `insumos`, `producto_insumo`, `movimientos_inventario` | Stock en tiempo real |
| Pedido y pago | `pedidos`, `pedido_detalle`, `pedido_detalle_opcion`, `pagos` | HU-04 y HU-05 |

El esquema vive en `lalucha` para no mezclarlo con `public`.

## Cómo levantarlo en Windows

1. Instalar [PostgreSQL 16]
2. Crear base y usuario (pgAdmin o `psql`):

```sql
CREATE USER lalucha_app WITH PASSWORD 'cambiar_esta_clave';
CREATE DATABASE lalucha OWNER lalucha_app;
GRANT ALL PRIVILEGES ON DATABASE lalucha TO lalucha_app;
```

3. Ejecutar scripts:

```bash
psql -U lalucha_app -d lalucha -f schema.sql
psql -U lalucha_app -d lalucha -f seed.sql
```

4. Verificación (evidencia):

```sql
SET search_path TO lalucha;

SELECT email, r.codigo AS rol
FROM usuarios u JOIN roles r ON r.id = u.rol_id;

SELECT c.nombre AS categoria, p.nombre, p.precio
FROM productos p
JOIN categorias c ON c.id = p.categoria_id
ORDER BY c.orden, p.nombre;

SELECT nombre, stock_actual, stock_minimo,
       CASE WHEN stock_actual <= stock_minimo THEN 'CRITICO' ELSE 'OK' END AS alerta
FROM insumos;
```

Usuario demo cliente: `juan.perez@example.com`  
Usuario demo admin: `ivan.p@example.net`  
La contraseña del hash semilla es un placeholder; en Spring Security el registro (HU-02) debe generar BCrypt real.