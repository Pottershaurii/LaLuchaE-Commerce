# LaLuchaE-Commerce

Plataforma mínima de e-commerce para **La Lucha Sanguchería** con:

- Consulta de menú para clientes
- Gestión de carrito
- Creación de pedidos en línea
- Gestión administrativa de productos y pedidos
- Control de inventario en tiempo real (actualización y descuento por pedido)

## Requisitos

- Node.js 18+

## Ejecutar

```bash
npm start
```

Aplicación: `http://localhost:3000`

## Probar

```bash
npm test
```

## Endpoints principales

### Cliente

- `GET /api/menu`
- `GET /api/cart/:cartId`
- `POST /api/cart/items`
- `DELETE /api/cart/:cartId/items/:productId`
- `POST /api/orders`

### Admin

- `GET /api/admin/products`
- `POST /api/admin/products`
- `PATCH /api/admin/products/:id`
- `PATCH /api/admin/inventory/:id`
- `GET /api/admin/orders`
- `PATCH /api/admin/orders/:id/status`
