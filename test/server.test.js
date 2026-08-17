const test = require('node:test');
const assert = require('node:assert/strict');
const { once } = require('node:events');
const { createApp } = require('../src/app');

async function startTestServer() {
  const { server } = createApp();
  server.listen(0);
  await once(server, 'listening');
  const { port } = server.address();
  return {
    server,
    baseUrl: `http://127.0.0.1:${port}`
  };
}

async function http(baseUrl, path, options) {
  const response = await fetch(`${baseUrl}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options
  });
  const data = await response.json();
  return { response, data };
}

test('menu, carrito y pedido descuentan inventario', async () => {
  const { server, baseUrl } = await startTestServer();
  try {
    const menu = await http(baseUrl, '/api/menu');
    assert.equal(menu.response.status, 200);
    assert.ok(menu.data.products.length >= 1);

    const item = menu.data.products[0];
    const add = await http(baseUrl, '/api/cart/items', {
      method: 'POST',
      body: JSON.stringify({ cartId: 'cart-test-1', productId: item.id, quantity: 2 })
    });
    assert.equal(add.response.status, 200);
    assert.equal(add.data.cart.items[0].quantity, 2);

    const order = await http(baseUrl, '/api/orders', {
      method: 'POST',
      body: JSON.stringify({ cartId: 'cart-test-1', customerName: 'Cliente QA' })
    });
    assert.equal(order.response.status, 201);
    assert.equal(order.data.order.status, 'pending');

    const products = await http(baseUrl, '/api/admin/products');
    const updated = products.data.products.find((p) => p.id === item.id);
    assert.equal(updated.stock, item.stock - 2);
  } finally {
    server.close();
  }
});

test('no permite exceder stock en carrito', async () => {
  const { server, baseUrl } = await startTestServer();
  try {
    const products = await http(baseUrl, '/api/admin/products');
    const lowStock = products.data.products[1];

    const over = await http(baseUrl, '/api/cart/items', {
      method: 'POST',
      body: JSON.stringify({ cartId: 'cart-test-2', productId: lowStock.id, quantity: lowStock.stock + 1 })
    });

    assert.equal(over.response.status, 409);
  } finally {
    server.close();
  }
});

test('admin puede actualizar inventario en tiempo real', async () => {
  const { server, baseUrl } = await startTestServer();
  try {
    const products = await http(baseUrl, '/api/admin/products');
    const product = products.data.products[2];

    const update = await http(baseUrl, `/api/admin/inventory/${product.id}`, {
      method: 'PATCH',
      body: JSON.stringify({ stock: 99 })
    });

    assert.equal(update.response.status, 200);
    assert.equal(update.data.product.stock, 99);

    const menu = await http(baseUrl, '/api/menu');
    const same = menu.data.products.find((p) => p.id === product.id);
    assert.equal(same.stock, 99);
  } finally {
    server.close();
  }
});
