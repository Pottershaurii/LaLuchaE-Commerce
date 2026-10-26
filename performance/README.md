# Plan de Capacidad y Rendimiento

**Proyecto:** La Lucha E-commerce · **Equipo:** Grupo 3 · **Curso:** Integrador II: Sistemas (UTP) · Semana 12

Este directorio contiene las herramientas con las que el equipo dimensiona y vigila la capacidad del sistema
(ITIL 4 – Gestión de la Capacidad y el Rendimiento; ISO/IEC 20000-1, 8.4.3).

| Archivo | Para qué sirve |
|---|---|
| `datos-volumen.sql` | Genera 3 años de datos sintéticos (50 000 clientes, 160 000 pedidos). **Solo para pruebas.** |
| `consultas-criticas.sql` | `EXPLAIN (ANALYZE, BUFFERS)` de las 3 consultas más críticas. |
| `k6/hora-pico.js` | Prueba de carga con Grafana k6: hora pico (20 TPS) y el doble (40 TPS). |
| `resumen.py` | Arma el resumen de resultados que se ve en GitHub Actions. |
| `../database/indices-rendimiento.sql` | Índices de llaves foráneas y búsquedas frecuentes (RFC-05). |
| `../.github/workflows/rendimiento.yml` | Ejecuta todo en una réplica con los límites del hardware de producción. |

## 1. Escenario de carga objetivo

| Variable | Valor | Sustento |
|---|---|---|
| Usuarios activos simultáneos en hora pico | **100** | Almuerzo y cena de fin de semana (estimación del equipo) |
| Ritmo por usuario | 1 petición cada 5 s | Navegación típica con tiempo de lectura |
| **TPS objetivo sostenido** | **20 peticiones/s** | 100 usuarios ÷ 5 s |
| TPS de estrés | 40 peticiones/s | Doble del pico, para medir el margen |
| Inicios de sesión | 2/s (4/s en estrés) | ~10 % del tráfico; es la operación más costosa (BCrypt) |
| Volumen de datos a 3 años | ~150 pedidos/día → 160 000 pedidos | Base de ~160 MB |

## 2. Hardware (Azure, grupo `rg-lalucha-dev`, Chile Central)

| Componente | Plan | vCPU | RAM | Almacenamiento |
|---|---|---|---|---|
| API `lalucha-api` (contenedor Docker) | App Service **B1** (por confirmar) | 1 | 1.75 GB | 10 GB |
| BD `lalucha-db` | PostgreSQL Flexible **B1ms** (por confirmar) | 1 (ráfaga) | 2 GB | 32 GB |

Respaldo: segunda instancia del API (escalado horizontal) cuando la CPU supere el umbral naranja de forma sostenida.
El pipeline `rendimiento.yml` reproduce estos límites con Docker (`--cpus 1 --memory 1792m` y `--cpus 1 --memory 2g`).

## 3. Pool de conexiones (HikariCP)

Configurado en `lalucha-backend/src/main/resources/application.properties`:

| Parámetro | Valor | Motivo |
|---|---|---|
| `maximum-pool-size` | 10 (`DB_POOL_MAX`) | B1ms admite 35 conexiones de usuario: 2 instancias × 10 = 20, quedan 15 libres |
| `minimum-idle` | 2 | Conexiones listas para el primer pico sin gastar recursos de noche |
| `connection-timeout` | 5 s | Falla rápido si el pool se agota, en lugar de colgar la petición |
| `idle-timeout` / `max-lifetime` | 5 min / 30 min | Renueva conexiones antes de que Azure las corte |
| `keepalive-time` | 2 min | Evita cortes de conexiones ociosas |
| `leak-detection-threshold` | 20 s | Registra en el log las conexiones que no se devuelven |

## 4. Umbrales críticos y alertas (Azure Monitor)

| Métrica (recurso) | Verde | 🟠 Alerta naranja | 🔴 Alerta roja | Acción |
|---|---|---|---|---|
| CPU del plan `CpuPercentage` (API) | < 60 % | ≥ 75 % por 10 min | ≥ 90 % por 5 min | Escalar a 2 instancias o a B2 |
| Memoria del plan `MemoryPercentage` (API) | < 70 % | ≥ 80 % por 10 min | ≥ 90 % por 5 min | Revisar heap de la JVM, reiniciar, subir plan |
| Tiempo de respuesta `HttpResponseTime` (API) | < 300 ms | > 300 ms promedio 5 min | > 500 ms promedio 5 min | Revisar consultas lentas y CPU |
| Errores `Http5xx` (API) | 0 | ≥ 5 en 5 min | ≥ 20 en 5 min | Revisar logs; plan de reversión si sigue |
| Health check `/api/health` | 100 % | — | < 100 % | Reinicio automático de la instancia |
| CPU `cpu_percent` (BD) | < 60 % | ≥ 75 % por 15 min | ≥ 90 % por 5 min | Revisar consultas con `pg_stat_statements` |
| Créditos de CPU `cpu_credits_remaining` (BD, ráfaga) | > 30 | < 30 | < 10 | Pasar a B2s antes de que se agoten |
| Conexiones `active_connections` (BD) | < 20 | ≥ 25 (70 % de 35) | ≥ 32 (90 %) | Revisar fugas del pool o bajar `DB_POOL_MAX` |
| Almacenamiento `storage_percent` (BD) | < 60 % | ≥ 75 % | ≥ 85 % | Ampliar almacenamiento |
| k6 p95 navegación (pipeline) | < 300 ms | — | ≥ 500 ms | El pipeline falla y el PR queda con el check en rojo |
| k6 errores (pipeline) | 0 % | — | ≥ 1 % | El pipeline falla y el PR queda con el check en rojo |

Las alertas de Azure Monitor las crea el administrador de Azure del equipo (RFC-06, cambio estándar) y notifican
por correo al Scrum Master y al administrador.

## 5. Cómo ejecutarlo

- **En GitHub:** Actions › *Capacidad y Rendimiento* › *Run workflow*. También corre cada lunes y en los PR que
  cambian estos archivos. El resultado queda en la pestaña *Summary* y en el artefacto `resultados-capacidad`.
- **En local** (con PostgreSQL y k6 instalados):

```bash
psql -d lalucha -f database/schema.sql -f database/seed.sql -f performance/datos-volumen.sql
psql -d lalucha -f performance/consultas-criticas.sql          # antes
psql -d lalucha -f database/indices-rendimiento.sql
psql -d lalucha -f performance/consultas-criticas.sql          # después
k6 run -e API_URL=http://localhost:8080 performance/k6/hora-pico.js
```

> `datos-volumen.sql` crea 50 000 usuarios de prueba: **no ejecutarlo en la base de producción.**
