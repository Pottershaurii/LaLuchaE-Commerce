// ============================================================================
// Prueba de carga "hora pico" de La Lucha E-commerce (Grafana k6)
//
// Escenario de negocio (Plan de Capacidad, Semana 12):
//   * Pico esperado: 100 usuarios activos a la vez (almuerzo y cena de fin de semana)
//     -> cada usuario hace ~1 peticion cada 5 s  = 20 peticiones/s (TPS)
//   * Prueba de estres: el doble del pico (40 TPS) para medir el margen.
//
// Dos flujos en paralelo, con tasa de llegada constante (open model):
//   navegacion     GET  /api/cliente/perfil  (JWT + consulta indexada por correo)
//   inicio_sesion  POST /api/auth/login      (BCrypt: la operacion mas costosa en CPU)
//
// Umbrales (si alguno se supera, k6 termina con error y el pipeline falla):
//   * menos de 1 % de errores
//   * p95 de navegacion < 500 ms   (umbral rojo de latencia)
//   * p95 de inicio de sesion < 800 ms
//
// Uso:  k6 run -e API_URL=http://localhost:8080 performance/k6/hora-pico.js
// ============================================================================
import http from 'k6/http';
import { check, fail } from 'k6';

const API = __ENV.API_URL || 'http://localhost:8080';
const CLAVE = __ENV.CLAVE_DEMO || 'Password123!';
const DOMINIO = __ENV.DOMINIO_CARGA || 'carga.lalucha.test';
const SESIONES = 40;      // clientes con sesion iniciada que navegan
const CLIENTES = 5000;    // universo de clientes que inician sesion
const JSON_HDR = { headers: { 'Content-Type': 'application/json' } };

// Rampa comun: 30 s subiendo al pico, 1 min sostenido, 30 s al doble, 1 min sostenido
const etapas = (pico) => [
  { target: pico, duration: '30s' },
  { target: pico, duration: '1m' },
  { target: pico * 2, duration: '30s' },
  { target: pico * 2, duration: '1m' },
  { target: 0, duration: '10s' },
];

export const options = {
  scenarios: {
    navegacion: {
      executor: 'ramping-arrival-rate',
      exec: 'navegar',
      startRate: 2, timeUnit: '1s',
      preAllocatedVUs: 40, maxVUs: 200,
      stages: etapas(18),           // 18 TPS de navegacion (x2 = 36)
    },
    inicio_sesion: {
      executor: 'ramping-arrival-rate',
      exec: 'iniciarSesion',
      startRate: 1, timeUnit: '1s',
      preAllocatedVUs: 20, maxVUs: 100,
      stages: etapas(2),            // 2 logins/s (x2 = 4)   -> total 20 TPS / 40 TPS
    },
  },
  thresholds: {
    'http_req_failed': ['rate<0.01'],
    'http_req_duration{scenario:navegacion}': ['p(95)<500'],
    'http_req_duration{scenario:inicio_sesion}': ['p(95)<800'],
    'checks': ['rate>0.99'],
    // Solo para que el resumen muestre cuantas peticiones hizo cada flujo
    'http_reqs{scenario:navegacion}': ['count>0'],
    'http_reqs{scenario:inicio_sesion}': ['count>0'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

function login(email) {
  return http.post(`${API}/api/auth/login`, JSON.stringify({ email, password: CLAVE }), JSON_HDR);
}

// Se ejecuta una sola vez: obtiene los JWT de los clientes que van a navegar
export function setup() {
  const salud = http.get(`${API}/api/health`);
  if (salud.status !== 200) fail(`El API no responde en ${API}/api/health (HTTP ${salud.status})`);
  const tokens = [];
  for (let i = 1; i <= SESIONES; i++) {
    const r = login(`cliente${i}@${DOMINIO}`);
    if (r.status === 200 && r.json('token')) tokens.push(r.json('token'));
  }
  if (tokens.length === 0) fail('No se pudo iniciar sesion con los clientes de carga (¿se cargo performance/datos-volumen.sql?)');
  return { tokens };
}

export function navegar(data) {
  const token = data.tokens[Math.floor(Math.random() * data.tokens.length)];
  const r = http.get(`${API}/api/cliente/perfil`, { headers: { Authorization: `Bearer ${token}` } });
  check(r, {
    'perfil responde 200': (res) => res.status === 200,
    'perfil trae el correo': (res) => (res.body || '').includes('@'),
  });
}

export function iniciarSesion() {
  const n = 1 + Math.floor(Math.random() * CLIENTES);
  const r = login(`cliente${n}@${DOMINIO}`);
  check(r, {
    'login responde 200': (res) => res.status === 200,
    'login entrega JWT': (res) => res.status === 200 && !!res.json('token'),
  });
}
