-- ============================================================================
-- HU-10 | Datos semilla para La Lucha Sangucheria
-- Ejecutar DESPUÉS de schema.sql
-- Password de usuarios demo: Password123!
-- Hash BCrypt (cost 10) compatible con Spring Security
-- ============================================================================

BEGIN;
SET search_path TO lalucha, public;

-- Roles
INSERT INTO roles (codigo, nombre, descripcion) VALUES
    ('ADMIN',   'Administrador', 'Panel de inventario, pedidos y usuarios'),
    ('CLIENTE', 'Cliente',       'Compra en la tienda en línea'),
    ('STAFF',   'Personal',      'Cocina / atención de pedidos');

-- Usuarios demo
-- Hash generado para 'Password123!'
INSERT INTO usuarios (rol_id, email, password_hash, nombres, apellidos, telefono, email_verificado, activo)
VALUES
    ((SELECT id FROM roles WHERE codigo = 'ADMIN'),
     'ivan.p@example.net',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
     'Rodrigo', 'Bello', '999000001', TRUE, TRUE),
    ((SELECT id FROM roles WHERE codigo = 'CLIENTE'),
     'juan.perez@example.com',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
     'Juan', 'Pérez', '999000002', TRUE, TRUE);

INSERT INTO clientes (usuario_id, dni, direccion, distrito, referencia)
VALUES (
    (SELECT id FROM usuarios WHERE email = 'juan.perez@example.com'),
    '12345678',
    'Av. La Marina 1234',
    'San Miguel',
    'Frente al parque'
);

-- Categorías del prototipo Figma
INSERT INTO categorias (codigo, nombre, descripcion, orden) VALUES
    ('SANGUCHES', 'Sánguches', 'Sánguches criollos tradicionales', 1),
    ('COMBOS',    'Combos',    'Sánguche + papas + bebida',        2),
    ('BEBIDAS',   'Bebidas',   'Jugos y chicha helada',            3),
    ('EXTRAS',    'Extras',    'Papas y adiciones',                4),
    ('POSTRES',   'Postres',   'Dulces de la casa',                5);

-- Productos
INSERT INTO productos
    (categoria_id, sku, nombre, slug, descripcion, precio, es_combo, es_personalizable, stock_minimo)
VALUES
    ((SELECT id FROM categorias WHERE codigo = 'SANGUCHES'),
     'SG-BUT-01', 'Sánguche de Butifarra', 'sanguche-butifarra',
     'Butifarra ahumada, cebolla encurtida y sarza criolla.', 14.90, FALSE, TRUE, 8),
    ((SELECT id FROM categorias WHERE codigo = 'SANGUCHES'),
     'SG-CHI-01', 'Sánguche de Chicharrón', 'sanguche-chicharron',
     'Chicharrón jugoso, camote y sarza criolla.', 17.90, FALSE, TRUE, 8),
    ((SELECT id FROM categorias WHERE codigo = 'SANGUCHES'),
     'SG-POL-01', 'Sánguche de Pollo', 'sanguche-pollo',
     'Pollo deshilachado, palta y mayonesa de la casa.', 15.90, FALSE, TRUE, 8),
    ((SELECT id FROM categorias WHERE codigo = 'SANGUCHES'),
     'SG-VEG-01', 'Sánguche Veggie', 'sanguche-veggie',
     'Portobello, palta, tomate y salsas de la casa.', 13.90, FALSE, TRUE, 6),
    ((SELECT id FROM categorias WHERE codigo = 'EXTRAS'),
     'EX-PAP-01', 'Papas huayro crocantes', 'papas-huayro',
     'Porción de papas huayro doradas.', 6.90, FALSE, FALSE, 15),
    ((SELECT id FROM categorias WHERE codigo = 'BEBIDAS'),
     'BB-CHI-01', 'Chicha helada', 'chicha-helada',
     'Chicha morada de la casa.', 5.90, FALSE, FALSE, 20),
    ((SELECT id FROM categorias WHERE codigo = 'BEBIDAS'),
     'BB-JUG-01', 'Jugo natural', 'jugo-natural',
     'Jugo del día (naranja, piña o surtido).', 7.90, FALSE, FALSE, 15),
    ((SELECT id FROM categorias WHERE codigo = 'POSTRES'),
     'PS-SUS-01', 'Suspiro limeño', 'suspiro-limeno',
     'Porción individual.', 8.90, FALSE, FALSE, 5),
    ((SELECT id FROM categorias WHERE codigo = 'COMBOS'),
     'CB-BUT-01', 'Combo Butifarra', 'combo-butifarra',
     'Butifarra + papas + chicha.', 19.90, TRUE, FALSE, 8),
    ((SELECT id FROM categorias WHERE codigo = 'COMBOS'),
     'CB-CHI-01', 'Combo Chicharrón', 'combo-chicharron',
     'Chicharrón + papas + chicha.', 22.90, TRUE, FALSE, 8),
    ((SELECT id FROM categorias WHERE codigo = 'COMBOS'),
     'CB-VEG-01', 'Combo Veggie', 'combo-veggie',
     'Veggie + papas + chicha.', 18.90, TRUE, FALSE, 6);

INSERT INTO combo_items (combo_id, producto_id, cantidad)
VALUES
    ((SELECT id FROM productos WHERE sku = 'CB-BUT-01'), (SELECT id FROM productos WHERE sku = 'SG-BUT-01'), 1),
    ((SELECT id FROM productos WHERE sku = 'CB-BUT-01'), (SELECT id FROM productos WHERE sku = 'EX-PAP-01'), 1),
    ((SELECT id FROM productos WHERE sku = 'CB-BUT-01'), (SELECT id FROM productos WHERE sku = 'BB-CHI-01'), 1),
    ((SELECT id FROM productos WHERE sku = 'CB-CHI-01'), (SELECT id FROM productos WHERE sku = 'SG-CHI-01'), 1),
    ((SELECT id FROM productos WHERE sku = 'CB-CHI-01'), (SELECT id FROM productos WHERE sku = 'EX-PAP-01'), 1),
    ((SELECT id FROM productos WHERE sku = 'CB-CHI-01'), (SELECT id FROM productos WHERE sku = 'BB-CHI-01'), 1),
    ((SELECT id FROM productos WHERE sku = 'CB-VEG-01'), (SELECT id FROM productos WHERE sku = 'SG-VEG-01'), 1),
    ((SELECT id FROM productos WHERE sku = 'CB-VEG-01'), (SELECT id FROM productos WHERE sku = 'EX-PAP-01'), 1),
    ((SELECT id FROM productos WHERE sku = 'CB-VEG-01'), (SELECT id FROM productos WHERE sku = 'BB-CHI-01'), 1);

-- Personalización HU-04
INSERT INTO grupos_opcion (codigo, nombre, obligatorio, max_seleccion) VALUES
    ('PAN',     'Tipo de pan',  TRUE,  1),
    ('SALSA',   'Salsas',       FALSE, 3),
    ('ADICION', 'Adiciones',    FALSE, 4);

INSERT INTO opciones (grupo_id, nombre, precio_extra) VALUES
    ((SELECT id FROM grupos_opcion WHERE codigo = 'PAN'), 'Francés',     0.00),
    ((SELECT id FROM grupos_opcion WHERE codigo = 'PAN'), 'Ciabatta',    1.50),
    ((SELECT id FROM grupos_opcion WHERE codigo = 'PAN'), 'Integral',    1.00),
    ((SELECT id FROM grupos_opcion WHERE codigo = 'SALSA'), 'Sarza criolla', 0.00),
    ((SELECT id FROM grupos_opcion WHERE codigo = 'SALSA'), 'Ají amarillo',  0.00),
    ((SELECT id FROM grupos_opcion WHERE codigo = 'SALSA'), 'Mayonesa de la casa', 0.00),
    ((SELECT id FROM grupos_opcion WHERE codigo = 'ADICION'), 'Palta extra',  2.50),
    ((SELECT id FROM grupos_opcion WHERE codigo = 'ADICION'), 'Queso',        2.00),
    ((SELECT id FROM grupos_opcion WHERE codigo = 'ADICION'), 'Huevo frito',  2.00);

INSERT INTO producto_opcion (producto_id, opcion_id)
SELECT p.id, o.id
FROM productos p
CROSS JOIN opciones o
WHERE p.es_personalizable = TRUE;

-- Inventario (insumos)
INSERT INTO insumos (codigo, nombre, unidad, stock_actual, stock_minimo, costo_unitario) VALUES
    ('INS-PAN',  'Pan francés',            'UND', 120, 30, 0.80),
    ('INS-BUT',  'Butifarra (porción)',    'UND',  40, 10, 3.20),
    ('INS-CHI',  'Chicharrón (porción)',   'UND',  35, 10, 4.50),
    ('INS-POL',  'Pollo (porción)',        'UND',  35, 10, 3.80),
    ('INS-VEG',  'Mix veggie (porción)',   'UND',  25,  8, 3.00),
    ('INS-PAP',  'Papa huayro (porción)',  'UND',  80, 20, 1.20),
    ('INS-CHI2', 'Chicha (vaso)',          'UND',  90, 20, 0.90),
    ('INS-JUG',  'Jugo natural (vaso)',    'UND',  40, 10, 1.50);

INSERT INTO producto_insumo (producto_id, insumo_id, cantidad) VALUES
    ((SELECT id FROM productos WHERE sku = 'SG-BUT-01'), (SELECT id FROM insumos WHERE codigo = 'INS-PAN'), 1),
    ((SELECT id FROM productos WHERE sku = 'SG-BUT-01'), (SELECT id FROM insumos WHERE codigo = 'INS-BUT'), 1),
    ((SELECT id FROM productos WHERE sku = 'SG-CHI-01'), (SELECT id FROM insumos WHERE codigo = 'INS-PAN'), 1),
    ((SELECT id FROM productos WHERE sku = 'SG-CHI-01'), (SELECT id FROM insumos WHERE codigo = 'INS-CHI'), 1),
    ((SELECT id FROM productos WHERE sku = 'SG-POL-01'), (SELECT id FROM insumos WHERE codigo = 'INS-PAN'), 1),
    ((SELECT id FROM productos WHERE sku = 'SG-POL-01'), (SELECT id FROM insumos WHERE codigo = 'INS-POL'), 1),
    ((SELECT id FROM productos WHERE sku = 'SG-VEG-01'), (SELECT id FROM insumos WHERE codigo = 'INS-PAN'), 1),
    ((SELECT id FROM productos WHERE sku = 'SG-VEG-01'), (SELECT id FROM insumos WHERE codigo = 'INS-VEG'), 1),
    ((SELECT id FROM productos WHERE sku = 'EX-PAP-01'), (SELECT id FROM insumos WHERE codigo = 'INS-PAP'), 1),
    ((SELECT id FROM productos WHERE sku = 'BB-CHI-01'), (SELECT id FROM insumos WHERE codigo = 'INS-CHI2'), 1),
    ((SELECT id FROM productos WHERE sku = 'BB-JUG-01'), (SELECT id FROM insumos WHERE codigo = 'INS-JUG'), 1);

COMMIT;

-- Consultas de verificación (evidencia HU-10)
-- SELECT * FROM lalucha.roles;
-- SELECT u.email, r.codigo AS rol FROM lalucha.usuarios u JOIN lalucha.roles r ON r.id = u.rol_id;
-- SELECT c.nombre AS categoria, p.nombre, p.precio FROM lalucha.productos p JOIN lalucha.categorias c ON c.id = p.categoria_id ORDER BY c.orden, p.nombre;
-- SELECT nombre, stock_actual, stock_minimo FROM lalucha.insumos;
