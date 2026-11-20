# Monitoreo y gestión de eventos con Prometheus y Grafana

**Proyecto:** La Lucha E-commerce · **Equipo:** Grupo 3 · **Curso:** Integrador II: Sistemas (UTP) · Semana 15, Sesión 30

Este documento acompaña las cuatro fichas de laboratorio de la Sesión 30. El resultado es un ciclo completo:
el API publica sus métricas, Prometheus las lee y evalúa reglas, Grafana las muestra y, cuando un umbral se
supera, Alertmanager abre un ticket P1 en el módulo de incidentes (Sesión 28) sin intervención manual.

```
lalucha-api (Spring Boot)          Prometheus                Alertmanager              lalucha-api
/actuator/prometheus  ◄── cada 15 s ──  reglas de alerta  ── FIRING ──►  webhook  ──►  POST /api/incidentes/alertas
                                          │                                           → ticket INC-### P1
                                          ▼
                                       Grafana (4 señales doradas)
```

## 1. Instrumentación del API (Ficha 01)

| Pieza | Cambio |
|---|---|
| `pom.xml` | `spring-boot-starter-actuator` y `micrometer-registry-prometheus` |
| `application.properties` | expone `health,info,prometheus`; histograma de latencia HTTP para el p95; desactiva el chequeo de Gmail |
| `SecurityConfig` | `GET /actuator/health` y `GET /actuator/prometheus` son públicos (solo lectura) |

Métricas principales: `process_cpu_usage`, `jvm_memory_used_bytes`, `jvm_threads_live_threads`,
`http_server_requests_seconds_*` (conteo, latencia y código de estado) y `hikaricp_connections_*` (pool de PostgreSQL).

Para ocultar las métricas en un entorno se define `ACTUATOR_EXPOSICION=health`.

## 2. Recolección y reglas (Ficha 02)

- `monitoring/prometheus.yml`: job `lalucha_api`, ruta `/actuator/prometheus`, cada 15 s.
- `monitoring/alert_rules.yml`: reglas con ventana sostenida (`for`) para evitar alertas por picos de segundos.

| Alerta | Condición | Ventana | Prioridad |
|---|---|---|---|
| ApiCaida | `up == 0` | 1 min | P1 |
| TasaErroresCritica5xx | 5xx > 5 % de las peticiones | 1 min | P1 |
| CpuCritica | CPU del proceso > 85 % | 1 min | P1 |
| PoolConexionesBDAgotado | conexiones activas > 90 % del pool | 1 min | P1 |
| LatenciaP95Alta | p95 > 500 ms (SLA) | 2 min | P2 |
| CpuAlta | CPU > 70 % | 2 min | P3 |

## 3. Tablero de Grafana (Ficha 03)

Se carga solo al iniciar (`monitoring/grafana/provisioning`): **UTP - Observabilidad Proyecto Final (La Lucha)**,
últimos 15 minutos, refresco cada 5 s.

| Señal | Panel | Consulta |
|---|---|---|
| Saturación | CPU y memoria heap (velocímetros; verde < 70 %, ámbar 70-85 %, rojo > 85 %) | `process_cpu_usage * 100` |
| Tráfico | Peticiones por segundo, total y por código | `sum(rate(http_server_requests_seconds_count[1m]))` |
| Latencia | p95 (verde < 0.3 s, ámbar 0.3-1 s, rojo > 1 s) | `histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket[1m])))` |
| Errores / estado | `up` (UP/DOWN), historial de alertas, % de 5xx | `up{job="lalucha_api"}`, `ALERTS{alertstate="firing"}` |

## 4. Alerta → ticket P1 (Ficha 04)

Alertmanager envía cada alerta al API (`POST /api/incidentes/alertas`) con el encabezado
`Authorization: Alerta <token>`. El API abre un ticket con la prioridad de la regla, asignado al Help Desk (N1),
y no repite el ticket mientras el anterior siga activo. Cuando la alerta se resuelve, el personal verifica y cierra el ticket.

Canal adicional opcional: en Grafana, *Alerting → Contact points → Add contact point → Discord*, pegar la URL del
webhook del canal del equipo y pulsar **Test**. La URL del webhook no se guarda en el repositorio.

## 5. Cómo ejecutarlo

### Opción A: en GitHub Actions (evidencias automáticas)

*Actions → Monitoreo y Gestion de Eventos → Run workflow.* En unos 15 minutos levanta la base, el API (1 vCPU, como
App Service B1), Prometheus, Alertmanager y Grafana; genera tráfico, lleva la CPU sobre 85 % con k6, detiene y
restablece el API, y guarda las capturas con fecha y hora en el artefacto **evidencias-monitoreo**.

### Opción B: en local, para la demostración en vivo (requiere Docker Desktop)

1. Levantar PostgreSQL y el API como siempre, con estas variables adicionales:
   `ALERTAS_WEBHOOK_TOKEN=<token>` y `ALERTAS_USUARIO_EMAIL=<correo de un usuario STAFF o ADMIN>`.
2. Guardar el mismo token en `monitoring/secretos/alertas_token` (la carpeta está en `.gitignore`).
3. Copiar `.env.example` a `.env` y definir `GRAFANA_ADMIN_PASSWORD`.
4. `docker compose -f docker-compose.monitoring.yml up -d`
5. Verificar: http://localhost:8080/actuator/prometheus, http://localhost:9090/targets (UP), http://localhost:3000.
6. Estrés: `k6 run -e API_URL=http://localhost:8080 -e VUS=40 -e DURACION=180s monitoring/script_carga.js`
   o caída: detener el API y volver a iniciarlo.

## 6. Seguridad

- Las métricas no contienen datos personales, pero revelan versiones y carga del sistema. En Azure se puede limitar
  `/actuator/prometheus` con restricciones de acceso por IP o apagarlo con `ACTUATOR_EXPOSICION=health`.
- El token del webhook y la clave de Grafana viven solo en variables de entorno y archivos ignorados por Git
  (Zero Hardcoding). El webhook compara el token en tiempo constante y responde 401 si no coincide.
