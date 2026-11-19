// ============================================================================
// Prueba de estres controlado - Sesion 30, Ficha 04 (Grafana k6)
// Mezcla inicios de sesion (BCrypt: lo mas costoso en CPU) y navegacion del
// catalogo para llevar la CPU del API sobre el 85 % y disparar la alerta P1.
//
// Uso:  k6 run -e API_URL=http://localhost:8080 -e VUS=40 -e DURACION=180s monitoring/script_carga.js
// Trafico base (sin estres): -e VUS=3 -e PAUSA=1
// ============================================================================
import http from 'k6/http';
import { check, sleep } from 'k6';

const API = __ENV.API_URL || 'http://localhost:8080';
const CLAVE = __ENV.CLAVE_DEMO || 'Password123!';
const PAUSA = Number(__ENV.PAUSA || 0);
const CLIENTES = ['juan.perez@example.com', 'ivan.p@example.net'];

export const options = {
  vus: Number(__ENV.VUS || 40),
  duration: __ENV.DURACION || '180s',
};

export default function () {
  const login = http.post(`${API}/api/auth/login`,
    JSON.stringify({ email: CLIENTES[__VU % CLIENTES.length], password: CLAVE }),
    { headers: { 'Content-Type': 'application/json' }, tags: { flujo: 'inicio_sesion' } });
  check(login, { 'login 200': (r) => r.status === 200 });

  const catalogo = http.get(`${API}/api/productos`, { tags: { flujo: 'catalogo' } });
  check(catalogo, { 'catalogo 200': (r) => r.status === 200 });

  if (PAUSA > 0) sleep(PAUSA);
}
