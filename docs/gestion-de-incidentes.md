# Gestión de Incidentes

**Proyecto:** La Lucha E-commerce · **Equipo:** Grupo 3 · **Curso:** Integrador II: Sistemas (UTP) · Semana 14, Sesión 28

Un incidente es una interrupción o degradación del servicio: la tienda no carga, un pago falla, un pedido no se registra.
Este módulo permite reportarlo desde la tienda, genera un ticket con código y hora, y deja al personal hacer el
seguimiento hasta cerrarlo, según la práctica de Gestión de Incidentes de ITIL 4.

## 1. Modelo de datos (`database/incidentes.sql`)

| Tabla | Para qué sirve |
|---|---|
| `tickets` | Registro principal: código `INC-###`, título, módulo afectado, descripción, estado, prioridad, quién reporta, a quién está asignado y fechas |
| `estados_ticket` | Ciclo de vida: 1 Abierto, 2 En diagnóstico, 3 Resuelto, 4 Cerrado |
| `prioridades` | Matriz impacto × urgencia con su tiempo objetivo de resolución |
| `asignados` | Grupos de soporte por nivel: Help Desk (N1), Especialista de base de datos (N2), Equipo de desarrollo (N3) |

La base de datos genera el código (`INC-001`, `INC-002`…) y la fecha de creación, así que no dependen del navegador.
Para una base ya instalada se ejecuta `database/incidentes.sql` (se puede correr dos veces sin error);
para deshacerlo, `database/incidentes-revertir.sql`.

## 2. Prioridad

| Prioridad | Impacto | Urgencia | Resolver en | Ejemplo |
|---|---|---|---|---|
| P1 · Crítico | Alto | Alta | 4 h | No se puede pagar ni iniciar sesión |
| P2 · Alto | Alto | Media | 8 h | El inventario no descuenta insumos |
| P3 · Normal | Medio | Media | 24 h | Un filtro del catálogo no funciona |
| P4 · Bajo | Bajo | Baja | 72 h | Un texto o imagen mal ubicado |

## 3. Procedimiento

1. **Registro:** cualquier usuario con sesión llena el formulario de la sección *Soporte* de la tienda
   (título, módulo afectado, descripción y prioridad). El ticket nace en estado *Abierto*, asignado al Help Desk.
2. **Diagnóstico:** el personal (STAFF o ADMIN) lo pasa a *En diagnóstico* desde el panel.
3. **Escalamiento:** si el Help Desk no lo resuelve, lo reasigna a nivel 2 (base de datos) o nivel 3 (desarrollo).
4. **Resolución:** se marca *Resuelto* cuando el servicio vuelve a funcionar.
5. **Cierre:** se marca *Cerrado* cuando quien reportó confirma; ahí sale del panel de tickets activos.

Si la solución exige cambiar código, base de datos o configuración, se abre un RFC (ver `docs/control-de-cambios.md`).

## 4. API

| Método | Ruta | Quién |
|---|---|---|
| `POST` | `/api/incidentes` | Cualquier usuario autenticado |
| `GET` | `/api/incidentes` | Autenticado: el personal ve todos los activos; el cliente, los suyos |
| `GET` | `/api/incidentes/catalogos` | Autenticado |
| `PATCH` | `/api/incidentes/{id}` | Solo STAFF o ADMIN (estado y asignación) |

## 5. Prueba integral

`IncidenteIntegrationTest` corre en el CI contra PostgreSQL: simula un fallo crítico en la pasarela de pagos y
comprueba que se genere el ticket con código `INC-###`, fecha, prioridad P1 y estado *Abierto*; que los datos
inválidos se rechacen con 400; que un cliente no pueda cambiar el estado (403); y que el personal lo vea en el
panel y lo escale a nivel 2.
