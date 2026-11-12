-- ============================================================================
-- Plan de reversion de RFC-05: deja los indices como en schema.sql
-- ============================================================================
SET search_path TO lalucha, public;

CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_pedidos_cliente ON pedidos (cliente_id);

DROP INDEX CONCURRENTLY IF EXISTS ix_pedido_detalle_pedido;
DROP INDEX CONCURRENTLY IF EXISTS ix_pedido_detalle_producto;
DROP INDEX CONCURRENTLY IF EXISTS ix_pedidos_cliente_fecha;
DROP INDEX CONCURRENTLY IF EXISTS ix_mov_pedido;
DROP INDEX CONCURRENTLY IF EXISTS ix_combo_items_producto;
DROP INDEX CONCURRENTLY IF EXISTS ix_opciones_grupo;
DROP INDEX CONCURRENTLY IF EXISTS ix_producto_opcion_opcion;
DROP INDEX CONCURRENTLY IF EXISTS ix_producto_insumo_insumo;
DROP INDEX CONCURRENTLY IF EXISTS ix_detalle_opcion_opcion;
