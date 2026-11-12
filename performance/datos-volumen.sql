-- ============================================================================
-- Datos sinteticos de VOLUMEN para pruebas de capacidad (NO usar en produccion)
-- Proyeccion a 3 anos de operacion de La Lucha:
--   * 50 000 clientes registrados
--   * ~150 pedidos/dia x 365 x 3  ->  160 000 pedidos
--   * 1 a 4 lineas por pedido      -> ~400 000 lineas de detalle
--   * 1 pago por pedido
-- Se carga DESPUES de schema.sql y seed.sql. Es deterministico (setseed).
-- Todos los usuarios usan la clave de demo Password123! (mismo hash del seed).
-- ============================================================================
BEGIN;
SET search_path TO lalucha, public;
SELECT setseed(0.42) AS semilla \gset

-- Usuarios CLIENTE verificados
INSERT INTO usuarios (rol_id, email, password_hash, nombres, apellidos, email_verificado, activo, created_at)
SELECT (SELECT id FROM roles WHERE codigo = 'CLIENTE'),
       'cliente' || g || '@carga.lalucha.test',
       '$2a$10$rWHVyXuEfnwt/OWo1Jj3ieWwajx7WL/r.2u11uoyRw3dN05K74knG',
       'Cliente', 'Carga ' || g, TRUE, TRUE,
       NOW() - (random() * INTERVAL '1095 days')
FROM generate_series(1, 50000) AS g;

INSERT INTO clientes (usuario_id, distrito)
SELECT u.id, (ARRAY['Miraflores','San Isidro','Surco','Barranco','San Borja','Lince'])[1 + floor(random() * 6)::int]
FROM usuarios u
WHERE u.email LIKE '%@carga.lalucha.test';

-- Pedidos repartidos en 3 anos (uno cada ~197 s); los mas recientes siguen en curso
INSERT INTO pedidos (codigo, cliente_id, estado, tipo_entrega, created_at)
SELECT 'LL-C-' || lpad(g::text, 7, '0'),
       r.cmin + floor(power(random(), 2) * (r.cmax - r.cmin + 1))::bigint,  -- distribucion sesgada: hay clientes frecuentes
       CASE WHEN g > 159800 THEN (ARRAY['PAGADO','EN_PREPARACION','LISTO'])[1 + floor(random() * 3)::int]
            WHEN random() < 0.04 THEN 'CANCELADO'
            ELSE 'ENTREGADO' END,
       CASE WHEN random() < 0.6 THEN 'DELIVERY' ELSE 'RECOJO' END,
       NOW() - ((160000 - g) * INTERVAL '197 seconds')
FROM generate_series(1, 160000) AS g,
     (SELECT min(id) AS cmin, max(id) AS cmax FROM clientes) r;

-- Detalle: de 1 a 4 productos por pedido, al precio del catalogo
INSERT INTO pedido_detalle (pedido_id, producto_id, cantidad, precio_unitario, subtotal)
SELECT x.pedido_id, pr.id, x.cant, pr.precio, pr.precio * x.cant
FROM (
    SELECT p.id AS pedido_id,
           1 + floor(random() * 3)::int AS cant,
           1 + floor(random() * (SELECT count(*) FROM productos))::int AS pos
    FROM pedidos p
    CROSS JOIN LATERAL generate_series(1, 1 + floor(random() * 4)::int + (p.id * 0)::int) AS l
) x
JOIN (SELECT id, precio, row_number() OVER (ORDER BY id) AS pos FROM productos) pr ON pr.pos = x.pos;

-- Totales del pedido y su pago
UPDATE pedidos p
SET subtotal = d.s,
    costo_envio = CASE WHEN p.tipo_entrega = 'DELIVERY' THEN 5 ELSE 0 END,
    total = d.s + CASE WHEN p.tipo_entrega = 'DELIVERY' THEN 5 ELSE 0 END
FROM (SELECT pedido_id, sum(subtotal) AS s FROM pedido_detalle GROUP BY pedido_id) d
WHERE d.pedido_id = p.id;

INSERT INTO pagos (pedido_id, metodo, estado, monto, pagado_en, created_at)
SELECT p.id,
       (ARRAY['TARJETA','YAPE','PLIN','EFECTIVO'])[1 + floor(random() * 4)::int],
       CASE WHEN p.estado = 'CANCELADO' THEN 'REEMBOLSADO' ELSE 'APROBADO' END,
       p.total, p.created_at, p.created_at
FROM pedidos p;

COMMIT;

ANALYZE;
