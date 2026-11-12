-- ============================================================================
-- Indices de rendimiento (Plan de Capacidad - Semana 12) | RFC-05
-- Motor: PostgreSQL 16+  |  Esquema: lalucha
--
-- Objetivo: que toda llave foranea y todo campo de busqueda frecuente tenga
-- indice, para evitar Full Table Scans cuando crezcan los pedidos.
--
-- * Idempotente (IF NOT EXISTS): se puede ejecutar mas de una vez.
-- * CONCURRENTLY: crea los indices sin bloquear escrituras en produccion.
--   Por eso NO va dentro de BEGIN/COMMIT: ejecutar con
--   psql -d lalucha -f database/indices-rendimiento.sql
-- * Reversion: database/indices-rendimiento-revertir.sql
-- ============================================================================
SET search_path TO lalucha, public;

-- C3 Detalle de pedido: pedido_detalle se filtra por pedido_id (antes Seq Scan)
CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_pedido_detalle_pedido   ON pedido_detalle (pedido_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_pedido_detalle_producto ON pedido_detalle (producto_id);

-- C2 Historial "Mis pedidos": filtra por cliente y ordena por fecha.
-- El indice compuesto entrega las filas ya ordenadas (sin paso Sort) y
-- reemplaza a ix_pedidos_cliente, que queda redundante.
CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_pedidos_cliente_fecha ON pedidos (cliente_id, created_at DESC);
DROP INDEX CONCURRENTLY IF EXISTS ix_pedidos_cliente;

-- Llaves foraneas que aun no tenian indice
CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_mov_pedido              ON movimientos_inventario (pedido_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_combo_items_producto    ON combo_items (producto_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_opciones_grupo          ON opciones (grupo_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_producto_opcion_opcion  ON producto_opcion (opcion_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_producto_insumo_insumo  ON producto_insumo (insumo_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_detalle_opcion_opcion   ON pedido_detalle_opcion (opcion_id);

-- Estadisticas al dia para que el planificador elija los indices
ANALYZE pedidos;
ANALYZE pedido_detalle;
