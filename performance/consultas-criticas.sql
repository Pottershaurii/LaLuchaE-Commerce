-- ============================================================================
-- Las 3 consultas mas criticas de La Lucha, con EXPLAIN (ANALYZE, BUFFERS).
-- Se ejecuta antes y despues de database/indices-rendimiento.sql para comparar.
-- Uso: psql -d lalucha -f performance/consultas-criticas.sql
-- ============================================================================
SET search_path TO lalucha, public;
\pset pager off

-- Valores de prueba: el cliente mas frecuente y su ultimo pedido
SELECT cliente_id, count(*) AS pedidos_cliente FROM pedidos
GROUP BY cliente_id ORDER BY count(*) DESC LIMIT 1 \gset
SELECT max(id) AS pedido_id FROM pedidos WHERE cliente_id = :cliente_id \gset
\echo 'Cliente de prueba:' :cliente_id 'con' :pedidos_cliente 'pedidos; pedido de prueba:' :pedido_id

\echo '### C1 - Login / perfil: buscar usuario por correo (UsuarioRepository.findByEmail)'
EXPLAIN (ANALYZE, BUFFERS, COSTS OFF, SUMMARY ON)
SELECT u.*, r.codigo
FROM usuarios u JOIN roles r ON r.id = u.rol_id
WHERE u.email = 'cliente25000@carga.lalucha.test';

\echo '### C2 - Historial "Mis pedidos": ultimos 20 pedidos del cliente'
EXPLAIN (ANALYZE, BUFFERS, COSTS OFF, SUMMARY ON)
SELECT id, codigo, estado, total, created_at
FROM pedidos
WHERE cliente_id = :cliente_id
ORDER BY created_at DESC
LIMIT 20;

\echo '### C3 - Detalle de un pedido con sus productos'
EXPLAIN (ANALYZE, BUFFERS, COSTS OFF, SUMMARY ON)
SELECT d.cantidad, d.precio_unitario, d.subtotal, p.nombre
FROM pedido_detalle d JOIN productos p ON p.id = d.producto_id
WHERE d.pedido_id = :pedido_id;
