-- ============================================================================
-- Gestión de Incidentes (Semana 14 - Sesión 28)  |  PostgreSQL 16+
-- Ticket + Estado + Prioridad (impacto x urgencia) + Asignado (nivel de soporte)
--
-- Se puede ejecutar sobre una base que ya existe (Azure): no borra datos y
-- puede correrse dos veces sin error. Para deshacerlo: incidentes-revertir.sql
-- Uso: psql -h <servidor> -U <usuario> -d <base> -v ON_ERROR_STOP=1 -f database/incidentes.sql
-- ============================================================================

BEGIN;

SET search_path TO lalucha, public;

-- Ciclo de vida del ticket. "activo" = se muestra en el panel de seguimiento.
CREATE TABLE IF NOT EXISTS estados_ticket (
    id      SMALLINT    PRIMARY KEY,
    nombre  VARCHAR(40) NOT NULL UNIQUE,
    activo  BOOLEAN     NOT NULL
);

-- Prioridad según la matriz impacto x urgencia, con su tiempo objetivo de resolución (SLA).
CREATE TABLE IF NOT EXISTS prioridades (
    id              SMALLINT    PRIMARY KEY,
    codigo          VARCHAR(2)  NOT NULL UNIQUE,
    nombre          VARCHAR(40) NOT NULL,
    impacto         VARCHAR(10) NOT NULL CHECK (impacto IN ('ALTO', 'MEDIO', 'BAJO')),
    urgencia        VARCHAR(10) NOT NULL CHECK (urgencia IN ('ALTA', 'MEDIA', 'BAJA')),
    horas_objetivo  INTEGER     NOT NULL CHECK (horas_objetivo > 0)
);

-- Grupos de soporte que pueden tener asignado un ticket (niveles 1, 2 y 3).
CREATE TABLE IF NOT EXISTS asignados (
    id          SMALLINT     PRIMARY KEY,
    nombre      VARCHAR(80)  NOT NULL UNIQUE,
    nivel       SMALLINT     NOT NULL CHECK (nivel BETWEEN 1 AND 3),
    descripcion VARCHAR(255)
);

-- Código legible del ticket: INC-001, INC-002 ... (crece a 4 dígitos sin repetirse)
CREATE SEQUENCE IF NOT EXISTS ticket_codigo_seq;

CREATE OR REPLACE FUNCTION siguiente_codigo_ticket()
RETURNS VARCHAR AS $$
    SELECT 'INC-' || LPAD(n::TEXT, GREATEST(3, LENGTH(n::TEXT)), '0')
    FROM (SELECT nextval('lalucha.ticket_codigo_seq') AS n) AS s;
$$ LANGUAGE sql;

CREATE TABLE IF NOT EXISTS tickets (
    id                  BIGSERIAL    PRIMARY KEY,
    codigo              VARCHAR(20)  NOT NULL UNIQUE DEFAULT siguiente_codigo_ticket(),
    titulo              VARCHAR(150) NOT NULL,
    modulo              VARCHAR(60)  NOT NULL,
    descripcion         TEXT         NOT NULL,
    estado_id           SMALLINT     NOT NULL DEFAULT 1 REFERENCES estados_ticket (id),
    prioridad_id        SMALLINT     NOT NULL REFERENCES prioridades (id),
    usuario_reporta_id  BIGINT       NOT NULL REFERENCES usuarios (id),
    asignado_id         SMALLINT     REFERENCES asignados (id),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS ix_tickets_estado    ON tickets (estado_id);
CREATE INDEX IF NOT EXISTS ix_tickets_prioridad ON tickets (prioridad_id);
CREATE INDEX IF NOT EXISTS ix_tickets_reporta   ON tickets (usuario_reporta_id);

DROP TRIGGER IF EXISTS trg_tickets_updated ON tickets;
CREATE TRIGGER trg_tickets_updated
    BEFORE UPDATE ON tickets FOR EACH ROW EXECUTE PROCEDURE set_updated_at();

-- Datos de referencia
INSERT INTO estados_ticket (id, nombre, activo) VALUES
    (1, 'Abierto',        TRUE),
    (2, 'En diagnóstico', TRUE),
    (3, 'Resuelto',       TRUE),
    (4, 'Cerrado',        FALSE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO prioridades (id, codigo, nombre, impacto, urgencia, horas_objetivo) VALUES
    (1, 'P1', 'Crítico', 'ALTO',  'ALTA',   4),
    (2, 'P2', 'Alto',    'ALTO',  'MEDIA',  8),
    (3, 'P3', 'Normal',  'MEDIO', 'MEDIA', 24),
    (4, 'P4', 'Bajo',    'BAJO',  'BAJA',  72)
ON CONFLICT (id) DO NOTHING;

INSERT INTO asignados (id, nombre, nivel, descripcion) VALUES
    (1, 'Help Desk',                     1, 'Recibe y clasifica todos los tickets; resuelve los casos conocidos'),
    (2, 'Especialista de base de datos', 2, 'Errores de datos, consultas lentas y conexiones a PostgreSQL'),
    (3, 'Equipo de desarrollo',          3, 'Errores de código, integraciones y despliegues')
ON CONFLICT (id) DO NOTHING;

COMMIT;
