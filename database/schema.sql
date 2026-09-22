-- ============================================================================
-- HU-10 | Plataforma E-commerce La Lucha
-- Motor: PostgreSQL 16+
-- ============================================================================
-- Convención:
--   * PK bigserial (Long en Java)
--   * timestamps created_at / updated_at
--   * soft-delete con activo BOOLEAN donde aplica
--   * dinero NUMERIC(10,2)
--   * enums como VARCHAR + CHECK
-- ============================================================================

BEGIN;

CREATE SCHEMA IF NOT EXISTS lalucha;
SET search_path TO lalucha, public;

-- ----------------------------------------------------------------------------
-- 1. SEGURIDAD Y PERSONAS  (HU-01, HU-02)
-- ----------------------------------------------------------------------------

CREATE TABLE roles (
    id              BIGSERIAL PRIMARY KEY,
    codigo          VARCHAR(30)  NOT NULL UNIQUE,  -- ADMIN, CLIENTE, STAFF
    nombre          VARCHAR(80)  NOT NULL,
    descripcion     VARCHAR(255),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE usuarios (
    id                  BIGSERIAL PRIMARY KEY,
    rol_id              BIGINT       NOT NULL REFERENCES roles(id),
    email               VARCHAR(180) NOT NULL,
    password_hash       VARCHAR(255) NOT NULL,          -- BCrypt (Spring Security)
    nombres             VARCHAR(120) NOT NULL,
    apellidos           VARCHAR(120) NOT NULL,
    telefono            VARCHAR(20),
    email_verificado    BOOLEAN      NOT NULL DEFAULT FALSE,
    token_verificacion  VARCHAR(120),
    activo              BOOLEAN      NOT NULL DEFAULT TRUE,
    ultimo_acceso       TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_usuarios_email UNIQUE (email)
);

CREATE INDEX ix_usuarios_rol    ON usuarios (rol_id);
CREATE INDEX ix_usuarios_activo ON usuarios (activo);

CREATE TABLE clientes (
    id                  BIGSERIAL PRIMARY KEY,
    usuario_id          BIGINT       NOT NULL UNIQUE REFERENCES usuarios(id),
    dni                 VARCHAR(15),
    fecha_nacimiento    DATE,
    direccion           VARCHAR(255),
    distrito            VARCHAR(80),
    referencia          VARCHAR(255),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_clientes_dni UNIQUE (dni)
);

-- ----------------------------------------------------------------------------
-- 2. CATÁLOGO  (HU-03)  — Sanguches, Combos, Bebidas, Extras, 
--    Postres + promociones
-- ----------------------------------------------------------------------------

CREATE TABLE categorias (
    id              BIGSERIAL PRIMARY KEY,
    codigo          VARCHAR(40)  NOT NULL UNIQUE,  -- SANGUCHES, COMBOS, BEBIDAS...
    nombre          VARCHAR(80)  NOT NULL,
    descripcion     VARCHAR(255),
    icono           VARCHAR(80),
    orden           INTEGER      NOT NULL DEFAULT 0,
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE productos (
    id                  BIGSERIAL PRIMARY KEY,
    categoria_id        BIGINT        NOT NULL REFERENCES categorias(id),
    sku                 VARCHAR(40)   NOT NULL UNIQUE,
    nombre              VARCHAR(120)  NOT NULL,
    slug                VARCHAR(140)  NOT NULL UNIQUE,
    descripcion         TEXT,
    precio              NUMERIC(10,2) NOT NULL CHECK (precio >= 0),
    precio_oferta       NUMERIC(10,2) CHECK (precio_oferta IS NULL OR precio_oferta >= 0),
    imagen_url          VARCHAR(500),
    es_combo            BOOLEAN       NOT NULL DEFAULT FALSE,
    es_personalizable   BOOLEAN       NOT NULL DEFAULT FALSE,  -- HU-04
    tiempo_prep_min     INTEGER,
    stock_minimo        INTEGER       NOT NULL DEFAULT 5,      -- alerta de stock crítico
    activo              BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX ix_productos_categoria ON productos (categoria_id);
CREATE INDEX ix_productos_activo    ON productos (activo);

-- Combos: que productos simples incluye un combo (Combo Butifarra = butifarra + papas + chicha)
CREATE TABLE combo_items (
    id                  BIGSERIAL PRIMARY KEY,
    combo_id            BIGINT  NOT NULL REFERENCES productos(id) ON DELETE CASCADE,
    producto_id         BIGINT  NOT NULL REFERENCES productos(id),
    cantidad            INTEGER NOT NULL DEFAULT 1 CHECK (cantidad > 0),
    CONSTRAINT uq_combo_item UNIQUE (combo_id, producto_id),
    CONSTRAINT ck_combo_no_self CHECK (combo_id <> producto_id)
);

-- ----------------------------------------------------------------------------
-- 3. PERSONALIZACIÓN DE PEDIDO  (HU-04): tipo de pan, salsas, adiciones
-- ----------------------------------------------------------------------------

CREATE TABLE grupos_opcion (
    id              BIGSERIAL PRIMARY KEY,
    codigo          VARCHAR(40)  NOT NULL UNIQUE,  -- PAN, SALSA, ADICION
    nombre          VARCHAR(80)  NOT NULL,
    obligatorio     BOOLEAN      NOT NULL DEFAULT FALSE,
    max_seleccion   INTEGER      NOT NULL DEFAULT 1 CHECK (max_seleccion > 0)
);

CREATE TABLE opciones (
    id                  BIGSERIAL PRIMARY KEY,
    grupo_id            BIGINT        NOT NULL REFERENCES grupos_opcion(id),
    nombre              VARCHAR(80)   NOT NULL,
    precio_extra        NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (precio_extra >= 0),
    activo              BOOLEAN       NOT NULL DEFAULT TRUE
);

CREATE TABLE producto_opcion (
    producto_id     BIGINT NOT NULL REFERENCES productos(id) ON DELETE CASCADE,
    opcion_id       BIGINT NOT NULL REFERENCES opciones(id)  ON DELETE CASCADE,
    PRIMARY KEY (producto_id, opcion_id)
);

-- ----------------------------------------------------------------------------
-- 4. INVENTARIO  (HU-10 núcleo + stock en tiempo real del README)
--    Se descuenta por RECETA al confirmar un pedido pagado.
-- ----------------------------------------------------------------------------

CREATE TABLE insumos (
    id                  BIGSERIAL PRIMARY KEY,
    codigo              VARCHAR(40)   NOT NULL UNIQUE,
    nombre              VARCHAR(120)  NOT NULL,
    unidad              VARCHAR(20)   NOT NULL,          -- UND, GR, ML
    stock_actual        NUMERIC(12,3) NOT NULL DEFAULT 0 CHECK (stock_actual >= 0),
    stock_minimo        NUMERIC(12,3) NOT NULL DEFAULT 0,
    costo_unitario      NUMERIC(10,2),
    activo              BOOLEAN       NOT NULL DEFAULT TRUE,
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- Receta: cuánto insumo consume 1 unidad de producto
CREATE TABLE producto_insumo (
    producto_id     BIGINT        NOT NULL REFERENCES productos(id) ON DELETE CASCADE,
    insumo_id       BIGINT        NOT NULL REFERENCES insumos(id),
    cantidad        NUMERIC(12,3) NOT NULL CHECK (cantidad > 0),
    PRIMARY KEY (producto_id, insumo_id)
);

CREATE TABLE movimientos_inventario (
    id              BIGSERIAL PRIMARY KEY,
    insumo_id       BIGINT        NOT NULL REFERENCES insumos(id),
    tipo            VARCHAR(20)   NOT NULL,
    cantidad        NUMERIC(12,3) NOT NULL CHECK (cantidad > 0),
    stock_resultante NUMERIC(12,3) NOT NULL,
    pedido_id       BIGINT,                              -- FK se agrega luego (circular)
    motivo          VARCHAR(255),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_mov_tipo CHECK (tipo IN ('ENTRADA', 'SALIDA', 'AJUSTE'))
);

CREATE INDEX ix_mov_insumo_fecha ON movimientos_inventario (insumo_id, created_at DESC);

-- ----------------------------------------------------------------------------
-- 5. PEDIDOS Y PAGOS  (HU-04, HU-05)
-- ----------------------------------------------------------------------------

CREATE TABLE pedidos (
    id                  BIGSERIAL PRIMARY KEY,
    codigo              VARCHAR(20)   NOT NULL UNIQUE,   -- LL-2026-0001
    cliente_id          BIGINT        NOT NULL REFERENCES clientes(id),
    estado              VARCHAR(20)   NOT NULL DEFAULT 'PENDIENTE',
    tipo_entrega        VARCHAR(20)   NOT NULL DEFAULT 'RECOJO',
    direccion_entrega   VARCHAR(255),
    subtotal            NUMERIC(10,2) NOT NULL DEFAULT 0,
    costo_envio         NUMERIC(10,2) NOT NULL DEFAULT 0,
    total               NUMERIC(10,2) NOT NULL DEFAULT 0,
    notas               VARCHAR(255),
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_pedido_estado CHECK (estado IN
        ('PENDIENTE', 'PAGADO', 'EN_PREPARACION', 'LISTO', 'ENVIADO', 'ENTREGADO', 'CANCELADO')),
    CONSTRAINT ck_pedido_entrega CHECK (tipo_entrega IN ('RECOJO', 'DELIVERY'))
);

CREATE INDEX ix_pedidos_cliente ON pedidos (cliente_id);
CREATE INDEX ix_pedidos_estado  ON pedidos (estado);

ALTER TABLE movimientos_inventario
    ADD CONSTRAINT fk_mov_pedido
    FOREIGN KEY (pedido_id) REFERENCES pedidos(id);

CREATE TABLE pedido_detalle (
    id              BIGSERIAL PRIMARY KEY,
    pedido_id       BIGINT        NOT NULL REFERENCES pedidos(id) ON DELETE CASCADE,
    producto_id     BIGINT        NOT NULL REFERENCES productos(id),
    cantidad        INTEGER       NOT NULL CHECK (cantidad > 0),
    precio_unitario NUMERIC(10,2) NOT NULL CHECK (precio_unitario >= 0),
    subtotal        NUMERIC(10,2) NOT NULL CHECK (subtotal >= 0)
);

CREATE TABLE pedido_detalle_opcion (
    detalle_id      BIGINT NOT NULL REFERENCES pedido_detalle(id) ON DELETE CASCADE,
    opcion_id       BIGINT NOT NULL REFERENCES opciones(id),
    precio_extra    NUMERIC(10,2) NOT NULL DEFAULT 0,
    PRIMARY KEY (detalle_id, opcion_id)
);

CREATE TABLE pagos (
    id                  BIGSERIAL PRIMARY KEY,
    pedido_id           BIGINT        NOT NULL REFERENCES pedidos(id),
    metodo              VARCHAR(30)   NOT NULL,          -- TARJETA, YAPE, PLIN, EFECTIVO
    estado              VARCHAR(20)   NOT NULL DEFAULT 'PENDIENTE',
    monto               NUMERIC(10,2) NOT NULL CHECK (monto >= 0),
    referencia_pasarela VARCHAR(120),
    pagado_en           TIMESTAMPTZ,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_pago_metodo CHECK (metodo IN ('TARJETA', 'YAPE', 'PLIN', 'EFECTIVO')),
    CONSTRAINT ck_pago_estado CHECK (estado IN ('PENDIENTE', 'APROBADO', 'RECHAZADO', 'REEMBOLSADO'))
);

CREATE INDEX ix_pagos_pedido ON pagos (pedido_id);

-- ----------------------------------------------------------------------------
-- 6. TRIGGER updated_at 
-- ----------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_usuarios_updated
    BEFORE UPDATE ON usuarios FOR EACH ROW EXECUTE PROCEDURE set_updated_at();
CREATE TRIGGER trg_clientes_updated
    BEFORE UPDATE ON clientes FOR EACH ROW EXECUTE PROCEDURE set_updated_at();
CREATE TRIGGER trg_productos_updated
    BEFORE UPDATE ON productos FOR EACH ROW EXECUTE PROCEDURE set_updated_at();
CREATE TRIGGER trg_pedidos_updated
    BEFORE UPDATE ON pedidos FOR EACH ROW EXECUTE PROCEDURE set_updated_at();

COMMIT;
