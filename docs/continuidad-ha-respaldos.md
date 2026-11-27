# Continuidad del servicio: alta disponibilidad, respaldos y DRP

**Proyecto:** La Lucha E-commerce · **Equipo:** Grupo 3 · **Curso:** Integrador II: Sistemas (UTP) · Semana 16, Sesiones 31 y 32
**Norma:** ISO/IEC 20000-1, cláusula 8.7 (gestión de la disponibilidad y continuidad del servicio)

## 1. Objetivos de continuidad (BIA, Sesión 31)

| Componente | Nivel | RPO | RTO | Estrategia |
|---|---|---|---|---|
| Base de datos PostgreSQL (Azure Flexible B1ms) | Tier 1 | 24 h (respaldo cifrado diario); hasta 5 min con la restauración a un punto en el tiempo de Azure | 15 min | Backup & Restore: respaldo diario fuera de Azure y restauración probada en sandbox |
| API Spring Boot (Azure App Service B1) | Tier 1 | 0 (no guarda estado; sesión con JWT) | 30 s | Activo-activo: 2 instancias detrás de un balanceador con chequeos de salud |
| Frontend React (Vite) | Tier 2 | 0 (código en Git) | 1 h | Volver a compilar y publicar desde Git |

## 2. Alta disponibilidad del API (Actividad 1)

`docker-compose.ha.yml` levanta dos réplicas del API (`api_node_1` en el puerto 8081 y `api_node_2` en el 8082,
cada una con 1 vCPU y 1.75 GB como un App Service B1) y un balanceador Nginx (`continuidad/nginx/nginx.conf`):

- `upstream backend_cluster` con `max_fails=2 fail_timeout=10s`: dos fallos seguidos sacan al nodo del grupo por 10 s.
- `proxy_next_upstream error timeout http_502 http_503 http_504`: si un nodo falla, la misma petición se reenvía al otro.
- `proxy_connect_timeout 1s`: un nodo sano conecta en milisegundos, así que un segundo sin respuesta basta para darlo por caído.
- Chequeo activo de Docker en cada réplica: `GET /api/health` cada 5 s.
- Cabeceras `X-Nodo` y `X-Served-By` y log de acceso con el nodo que atendió cada petición.

Como el API usa JWT y no guarda sesión en memoria, no hace falta afinidad: un token emitido por un nodo lo acepta el otro.
Con 2 réplicas el pool de conexiones suma 2 × 10 = 20 de las 35 que admite PostgreSQL Flexible B1ms.

**Equivalente en Azure:** escalar el App Service a 2 instancias (*Scale out*) y activar *Health check* en `/api/health`.
El balanceador de App Service reparte el tráfico y saca a la instancia que falla. Lo configura quien administra Azure.

## 3. Respaldos automáticos (Actividad 2)

`.github/workflows/respaldos.yml` corre todos los días a las 02:00 (hora de Lima):

1. `continuidad/backup/respaldo.sh`: `pg_dump | gzip -9 | openssl enc -aes-256-cbc -pbkdf2 -iter 200000`, con huella SHA-256
   y un manifiesto con las filas de cada tabla dentro del respaldo.
2. Sube el respaldo cifrado como artefacto de GitHub (`respaldo-bd-N`) con retención de 14 días.
3. Borra la copia local, descarga el artefacto, lo restaura en un PostgreSQL limpio (`continuidad/backup/restaurar.sh`)
   y compara las filas tabla por tabla. Si algo no coincide o pasa de 15 min, el workflow falla.

Secretos del repositorio (los crea un administrador en *Settings → Secrets and variables → Actions*; nunca en el código):

| Secreto | Uso |
|---|---|
| `BACKUP_PASS` | Clave de cifrado AES-256. Sin ella cada corrida usa una clave efímera y el respaldo solo sirve para el simulacro. |
| `BACKUP_PGHOST`, `BACKUP_PGUSER`, `BACKUP_PGPASSWORD`, `BACKUP_PGDATABASE` | Conexión a la BD de Azure. Sin ellos se respalda una réplica efímera con el schema, el seed y 3 años de datos sintéticos. |

Para respaldar Azure también hay que permitir la conexión desde GitHub Actions en el firewall de PostgreSQL Flexible.

### Regla 3-2-1-1-0

| Criterio | Cómo se cumple | Estado |
|---|---|---|
| 3 copias | BD de producción + respaldos automáticos de Azure (restauración a un punto en el tiempo, 7 días) + respaldo cifrado en GitHub | Cumple |
| 2 medios | Almacenamiento gestionado de Azure y almacenamiento de artefactos de GitHub | Cumple |
| 1 fuera del sitio | GitHub, otro proveedor: si se pierde la suscripción de Azure, el respaldo sigue disponible | Cumple |
| 1 inmutable | El artefacto no se puede modificar después de subido, pero un administrador puede borrarlo antes de que venza. Para inmutabilidad real (WORM): contenedor de Azure Blob con política de retención por tiempo de 14 días en una cuenta aparte | Parcial |
| 0 errores | Cada corrida restaura y verifica el respaldo; un fallo deja el workflow en rojo | Cumple |

## 4. Runbook DRP: corrupción lógica o ransomware en la base de datos

**Disparador:** alerta `TasaErroresCritica5xx` de Prometheus (5xx > 5 % durante 1 min, Sesión 30). Alertmanager abre un ticket P1
en el módulo de incidentes.

| Fase | Acción | Rol | Min |
|---|---|---|---|
| 1. Detección y aislamiento | Confirmar el daño (`SELECT count(*)` en tablas clave, log del API); detener el App Service (`az webapp stop`) para que no se escriban más datos; cambiar la contraseña del usuario de la BD | Arquitecto HA | 3 |
| 2. Aprovisionamiento alterno | Crear una BD limpia `lalucha_restaurada` en el mismo servidor (`az postgres flexible-server db create`) o restaurar el servidor a un punto anterior al incidente (`az postgres flexible-server restore --restore-time`) | Ing. de Automatización de Backups | 4 |
| 3. Restauración de datos | `gh run download -n respaldo-bd-N`; `sha256sum -c`; `continuidad/backup/restaurar.sh` contra la BD nueva | Ing. de Automatización de Backups | 5 |
| 4. Despliegue | Apuntar `SPRING_DATASOURCE_URL` a la BD restaurada (`az webapp config appsettings set`) e iniciar el App Service; si la causa fue un cambio, volver a la imagen anterior con el workflow *Deploy lalucha-api* | Arquitecto HA | 2 |
| 5. Validación y apertura | `/api/health` en 200; filas por tabla iguales al manifiesto; login y pedido de prueba; la alerta pasa a RESOLVED; ticket a Resuelto y RFC con la causa raíz | Ingeniero QA | 1 |

Total estimado: 15 min (RTO comprometido). Pérdida máxima de datos: lo registrado desde el último respaldo (RPO 24 h), o
hasta 5 min si se usa la restauración a un punto en el tiempo de Azure.

## 5. Cómo ejecutar las pruebas

### En GitHub Actions (evidencias automáticas)

- *Actions → Continuidad - Alta Disponibilidad y Failover → Run workflow*: levanta las 2 réplicas y Nginx, comprueba el reparto,
  detiene `api_node_1` con tráfico activo y mide el RTO del failover. Capturas en el artefacto **evidencias-continuidad-ha**.
- *Actions → Respaldos de Base de Datos → Run workflow*: respalda, sube fuera del sitio, restaura en sandbox y mide el RTO.
  Capturas en el artefacto **evidencias-respaldos**.

### En local (demostración en vivo, requiere Docker Desktop)

1. Copiar `.env.example` a `.env` y definir `DB_PASSWORD`, `JWT_SECRET` y `MAIL_PASSWORD` (sin correo real basta cualquier valor).
2. `docker compose -f docker-compose.ha.yml up -d --build --wait`
3. Reparto: `curl -i http://localhost:8088/api/productos` varias veces y mirar la cabecera `X-Nodo`.
4. Failover: `python continuidad/ha/prueba_failover.py --url http://localhost:8088/api/productos`
   (o a mano: `docker stop api_node_1`, seguir llamando al API y `docker start api_node_1`).
5. Respaldo y restore (Git Bash o WSL, con `BACKUP_PASS` definida en la terminal):
   `PGHOST=localhost PGPORT=5433 PGUSER=postgres PGDATABASE=lalucha bash continuidad/backup/respaldo.sh`
   y `PGHOST=localhost PGPORT=5433 PGUSER=postgres bash continuidad/backup/restaurar.sh respaldos`
