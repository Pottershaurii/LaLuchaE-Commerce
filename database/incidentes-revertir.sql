-- Reversión del módulo de incidentes (plan de reversión del RFC-08).
-- Borra los tickets registrados: sacar antes un respaldo con
--   pg_dump -t lalucha.tickets ... > tickets_respaldo.sql
BEGIN;

SET search_path TO lalucha, public;

DROP TABLE IF EXISTS tickets;
DROP FUNCTION IF EXISTS siguiente_codigo_ticket();
DROP SEQUENCE IF EXISTS ticket_codigo_seq;
DROP TABLE IF EXISTS asignados;
DROP TABLE IF EXISTS prioridades;
DROP TABLE IF EXISTS estados_ticket;

COMMIT;
