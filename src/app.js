const http = require('node:http');
const { randomUUID } = require('node:crypto');
const { readFile } = require('node:fs/promises');

const ORDER_STATUSES = new Set(['pending', 'preparing', 'ready', 'delivered', 'cancelled']);

function createInitialState() {
  return {
    products: [
      { id: 'p1', name: 'Sánguche de Chicharrón', description: 'Pan artesanal, camote y cebolla criolla', price: 18.5, stock: 20, active: true },
      { id: 'p2', name: 'Sánguche de Asado', description: 'Asado cocido a fuego lento con salsa criolla', price: 20.0, stock: 15, active: true },
      { id: 'p3', name: 'Jugo de Maracuyá', description: 'Bebida natural de maracuyá', price: 9.0, stock: 30, active: true }
    ],
    carts: new Map(),
    orders: []
  };
}

function sendJson(res, statusCode, payload) {
  const body = JSON.stringify(payload);
  res.writeHead(statusCode, {
    'Content-Type': 'application/json; charset=utf-8',
    'Content-Length': Buffer.byteLength(body)
  });
  res.end(body);
}

async function readJsonBody(req) {
  let raw = '';
  for await (const chunk of req) {
    raw += chunk;
    if (raw.length > 1_000_000) {
      throw new Error('Request body too large');
    }
  }
  if (!raw) return {};
  return JSON.parse(raw);
}

function parsePath(pathname) {
  return pathname.split('/').filter(Boolean);
}

function getOrCreateCart(state, cartId) {
  if (!state.carts.has(cartId)) {
    state.carts.set(cartId, { id: cartId, items: new Map() });
  }
  return state.carts.get(cartId);
}

function getProductById(state, productId) {
  return state.products.find((p) => p.id === productId);
}

function serializeCart(state, cart) {
  const items = [];
  let total = 0;

  for (const [productId, quantity] of cart.items.entries()) {
    const product = getProductById(state, productId);
    if (!product) continue;
    const subtotal = Number((product.price * quantity).toFixed(2));
    total += subtotal;
    items.push({
      productId,
      name: product.name,
      price: product.price,
      quantity,
      subtotal
    });
  }

  return {
    id: cart.id,
    items,
    total: Number(total.toFixed(2))
  };
}

async function serveHome(res) {
  const html = await readFile(new URL('../public/index.html', `file://${__filename}`), 'utf-8');
  res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
  res.end(html);
}

function createApp(state = createInitialState()) {
  const server = http.createServer(async (req, res) => {
    try {
      const reqUrl = new URL(req.url, 'http://localhost');
      const parts = parsePath(reqUrl.pathname);

      if (req.method === 'GET' && (reqUrl.pathname === '/' || reqUrl.pathname === '/index.html')) {
        await serveHome(res);
        return;
      }

      if (req.method === 'GET' && reqUrl.pathname === '/api/menu') {
        const menu = state.products.filter((p) => p.active);
        sendJson(res, 200, { products: menu });
        return;
      }

      if (req.method === 'POST' && reqUrl.pathname === '/api/cart/items') {
        const body = await readJsonBody(req);
        const { cartId, productId } = body;
        const quantity = Number(body.quantity ?? 1);

        if (!cartId || !productId || !Number.isInteger(quantity) || quantity <= 0) {
          sendJson(res, 400, { error: 'cartId, productId y quantity (>0) son obligatorios.' });
          return;
        }

        const product = getProductById(state, productId);
        if (!product || !product.active) {
          sendJson(res, 404, { error: 'Producto no encontrado o inactivo.' });
          return;
        }

        const cart = getOrCreateCart(state, cartId);
        const currentQuantity = cart.items.get(productId) || 0;

        if (currentQuantity + quantity > product.stock) {
          sendJson(res, 409, { error: 'Stock insuficiente para agregar al carrito.' });
          return;
        }

        cart.items.set(productId, currentQuantity + quantity);
        sendJson(res, 200, { cart: serializeCart(state, cart) });
        return;
      }

      if (req.method === 'GET' && parts[0] === 'api' && parts[1] === 'cart' && parts.length === 3) {
        const cart = getOrCreateCart(state, parts[2]);
        sendJson(res, 200, { cart: serializeCart(state, cart) });
        return;
      }

      if (req.method === 'DELETE' && parts[0] === 'api' && parts[1] === 'cart' && parts[3] === 'items' && parts.length === 5) {
        const cart = getOrCreateCart(state, parts[2]);
        cart.items.delete(parts[4]);
        sendJson(res, 200, { cart: serializeCart(state, cart) });
        return;
      }

      if (req.method === 'POST' && reqUrl.pathname === '/api/orders') {
        const body = await readJsonBody(req);
        const cartId = body.cartId;
        const customerName = String(body.customerName || '').trim();

        if (!cartId || !customerName) {
          sendJson(res, 400, { error: 'cartId y customerName son obligatorios.' });
          return;
        }

        const cart = getOrCreateCart(state, cartId);
        if (cart.items.size === 0) {
          sendJson(res, 400, { error: 'El carrito está vacío.' });
          return;
        }

        const orderItems = [];
        for (const [productId, quantity] of cart.items.entries()) {
          const product = getProductById(state, productId);
          if (!product || !product.active) {
            sendJson(res, 400, { error: `Producto inválido en carrito: ${productId}` });
            return;
          }
          if (product.stock < quantity) {
            sendJson(res, 409, { error: `Stock insuficiente para ${product.name}` });
            return;
          }
          orderItems.push({ product, quantity });
        }

        let total = 0;
        for (const { product, quantity } of orderItems) {
          product.stock -= quantity;
          total += product.price * quantity;
        }

        const order = {
          id: randomUUID(),
          customerName,
          status: 'pending',
          createdAt: new Date().toISOString(),
          items: orderItems.map(({ product, quantity }) => ({
            productId: product.id,
            name: product.name,
            quantity,
            unitPrice: product.price,
            subtotal: Number((product.price * quantity).toFixed(2))
          })),
          total: Number(total.toFixed(2))
        };

        state.orders.push(order);
        cart.items.clear();

        sendJson(res, 201, { order });
        return;
      }

      if (req.method === 'GET' && reqUrl.pathname === '/api/admin/products') {
        sendJson(res, 200, { products: state.products });
        return;
      }

      if (req.method === 'POST' && reqUrl.pathname === '/api/admin/products') {
        const body = await readJsonBody(req);
        const name = String(body.name || '').trim();
        const description = String(body.description || '').trim();
        const price = Number(body.price);
        const stock = Number(body.stock);

        if (!name || !Number.isFinite(price) || price <= 0 || !Number.isInteger(stock) || stock < 0) {
          sendJson(res, 400, { error: 'Datos inválidos para crear producto.' });
          return;
        }

        const product = {
          id: randomUUID(),
          name,
          description,
          price: Number(price.toFixed(2)),
          stock,
          active: body.active !== false
        };

        state.products.push(product);
        sendJson(res, 201, { product });
        return;
      }

      if (req.method === 'PATCH' && parts[0] === 'api' && parts[1] === 'admin' && parts[2] === 'products' && parts.length === 4) {
        const body = await readJsonBody(req);
        const product = getProductById(state, parts[3]);

        if (!product) {
          sendJson(res, 404, { error: 'Producto no encontrado.' });
          return;
        }

        if (body.name !== undefined) {
          const name = String(body.name).trim();
          if (!name) {
            sendJson(res, 400, { error: 'name no puede estar vacío.' });
            return;
          }
          product.name = name;
        }

        if (body.description !== undefined) {
          product.description = String(body.description).trim();
        }

        if (body.price !== undefined) {
          const price = Number(body.price);
          if (!Number.isFinite(price) || price <= 0) {
            sendJson(res, 400, { error: 'price inválido.' });
            return;
          }
          product.price = Number(price.toFixed(2));
        }

        if (body.stock !== undefined) {
          const stock = Number(body.stock);
          if (!Number.isInteger(stock) || stock < 0) {
            sendJson(res, 400, { error: 'stock inválido.' });
            return;
          }
          product.stock = stock;
        }

        if (body.active !== undefined) {
          product.active = Boolean(body.active);
        }

        sendJson(res, 200, { product });
        return;
      }

      if (req.method === 'PATCH' && parts[0] === 'api' && parts[1] === 'admin' && parts[2] === 'inventory' && parts.length === 4) {
        const body = await readJsonBody(req);
        const stock = Number(body.stock);
        const product = getProductById(state, parts[3]);

        if (!product) {
          sendJson(res, 404, { error: 'Producto no encontrado.' });
          return;
        }

        if (!Number.isInteger(stock) || stock < 0) {
          sendJson(res, 400, { error: 'stock inválido.' });
          return;
        }

        product.stock = stock;
        sendJson(res, 200, { product });
        return;
      }

      if (req.method === 'GET' && reqUrl.pathname === '/api/admin/orders') {
        sendJson(res, 200, { orders: state.orders });
        return;
      }

      if (req.method === 'PATCH' && parts[0] === 'api' && parts[1] === 'admin' && parts[2] === 'orders' && parts[4] === 'status' && parts.length === 6) {
        const body = await readJsonBody(req);
        const status = String(body.status || '').trim();
        const order = state.orders.find((o) => o.id === parts[3]);

        if (!order) {
          sendJson(res, 404, { error: 'Pedido no encontrado.' });
          return;
        }

        if (!ORDER_STATUSES.has(status)) {
          sendJson(res, 400, { error: 'Estado de pedido inválido.' });
          return;
        }

        order.status = status;
        sendJson(res, 200, { order });
        return;
      }

      sendJson(res, 404, { error: 'Ruta no encontrada.' });
    } catch (error) {
      const statusCode = error instanceof SyntaxError ? 400 : 500;
      sendJson(res, statusCode, { error: 'Error procesando la solicitud.' });
    }
  });

  return { server, state };
}

module.exports = { createApp, createInitialState };
