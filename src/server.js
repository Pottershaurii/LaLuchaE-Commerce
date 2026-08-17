const { createApp } = require('./app');

const { server } = createApp();
const port = Number(process.env.PORT) || 3000;

server.listen(port, () => {
  console.log(`La Lucha e-commerce disponible en http://localhost:${port}`);
});
