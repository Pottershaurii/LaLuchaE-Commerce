# Control de Cambios e Integracion Continua

**Proyecto:** La Lucha E-commerce · **Equipo:** Grupo 3 · **Curso:** Integrador II: Sistemas (UTP)

Este documento describe como el equipo integra, valida y libera cambios en el sistema, siguiendo las practicas de
gestion de cambios de ITIL 4 y el principio de segregacion de funciones (SoD).

---

## 1. Estructura del repositorio

| Carpeta | Contenido |
|---|---|
| `lalucha-backend/` | API REST en Java 17 + Spring Boot 3.2 (Dockerfile incluido) |
| `frontend-react/` | Tienda web en React 19 + Vite |
| `database/` | Esquema `schema.sql`, datos semilla `seed.sql` y guia de instalacion |
| `docs/` | Documentacion del proceso (este archivo) |
| `.github/workflows/` | Pipelines de integracion y despliegue continuo |
| `.github/ISSUE_TEMPLATE/` | Plantilla de Solicitud de Cambio (RFC) |

## 2. Ramas y proteccion de `main`

- `main` es la rama de produccion: todo lo que entra se despliega en Azure.
- Nadie sube cambios directamente a `main`. Cada cambio se trabaja en una rama
  (`feature/...`, `fix/...`, `ci/...`) y entra por **pull request**.
- Reglas de proteccion configuradas en GitHub:
  - Pull request obligatorio, con **1 aprobacion** de un integrante distinto al autor.
  - El **CI Pipeline** debe terminar en verde (jobs *Backend* y *Frontend*).
  - Se bloquean el *force push* y el borrado de la rama.

## 3. Segregacion de funciones (SoD)

| Rol | Puede | No puede |
|---|---|---|
| Desarrollador (Dev Team) | Crear ramas, abrir PR y RFC | Aprobar su propio PR ni subir directo a `main` |
| Revisor (otro integrante) | Revisar y aprobar el PR | Aprobar un PR que el mismo escribio |
| Product Owner | Aprobar RFC de tipo Normal | Implementar el cambio que aprueba |
| Pipeline CI/CD | Compilar, probar y desplegar | Desplegar si el CI no paso |
| Administrador de Azure | Gestionar variables de entorno y secretos | Modificar el codigo sin PR |

Los secretos (perfil de publicacion de Azure, credenciales de BD, `JWT_SECRET`, correo) viven en
**GitHub Secrets** y en las **variables de entorno de Azure**, nunca en el codigo.

## 4. Pipeline de Integracion Continua (`ci-pipeline.yml`)

Se ejecuta en cada `push` y `pull_request` hacia `main`:

1. **Backend (Java 17 + Maven):** levanta un PostgreSQL 16 temporal, carga `schema.sql` y `seed.sql`,
   compila con `./mvnw clean compile` y ejecuta las pruebas con `./mvnw test`
   (JUnit 5, Mockito y MockMvc, incluidas las pruebas de seguridad OWASP A01 y A03).
2. **Frontend (Node 22 + Vite):** instala dependencias, revisa el codigo con ESLint y genera el build de produccion.

Cada ejecucion publica un resumen con el resultado de las pruebas en la pestana *Summary* de GitHub Actions.

El despliegue (`deploy-lalucha-api.yml`) **solo se dispara cuando el CI termina en verde sobre `main`**,
y publica la imagen Docker etiquetada con el SHA del commit.

## 5. Flujo de un cambio

```
RFC (Issue) ─► rama + PR ─► CI Pipeline en verde ─► aprobacion de otro integrante ─► merge a main
                                                                                      │
                               Azure App Service ◄── Deploy (imagen :SHA) ◄── CI en verde sobre main
```

## 6. Tipos de cambio (ITIL 4) aplicados al Sprint 5

| RFC | Cambio | Tipo | Por que |
|---|---|---|---|
| RFC-01 | Pipeline CI unificado y despliegue condicionado al CI con imagen versionada | Normal | Modifica como se libera a produccion; requiere evaluacion y aprobacion |
| RFC-02 | Modulo de gestion de solicitudes de servicio (S5-HU-01 a S5-HU-03) | Normal | Agrega tablas al esquema y nuevos endpoints protegidos |
| RFC-03 | Configurar `MAIL_USERNAME` y `MAIL_PASSWORD` en Azure | Estandar | Configuracion documentada, de bajo riesgo y repetible |
| RFC-04 | Habilitar CORS para el origen de la tienda publicada (PR #48) | Emergencia | La tienda en produccion no podia registrar ni iniciar sesion |

## 7. Plan de reversion (Backout Plan) de la version APF3

**Cuando se activa:** si despues del despliegue ocurre cualquiera de estas condiciones durante la verificacion
(primeros 30 minutos):

- `GET /api/health` no responde `{"status":"UP"}`.
- El inicio de sesion o el registro fallan con errores 5xx.
- Las pruebas de humo de seguridad no devuelven 401/403 donde corresponde.

**Como se revierte:**

| Componente | Accion | Tiempo objetivo |
|---|---|---|
| Backend (`lalucha-api`) | GitHub › Actions › *Deploy lalucha-api* › *Run workflow* con `image_tag` = SHA de la ultima version estable. Se redespliega esa imagen ya publicada, sin recompilar. | 10 minutos |
| Codigo en `main` | `git revert` del merge problematico mediante un PR de emergencia (el historial no se reescribe). | 15 minutos |
| Base de datos | Antes de cualquier cambio de esquema se toma un respaldo (`pg_dump`). Si el cambio falla, se restaura el respaldo o se usa la restauracion a un punto en el tiempo de Azure Database for PostgreSQL. | 30 minutos |
| Frontend (`lalucha-web`) | Se vuelve a publicar el build de la version anterior. | 15 minutos |

**Responsables:** el Scrum Master decide la reversion; un integrante distinto de quien implemento el cambio la ejecuta,
y el resultado se registra en el RFC correspondiente.

**Version estable de referencia:** se registra en el RFC el SHA de la imagen desplegada antes del cambio
(se ve en el *Summary* del workflow *Deploy lalucha-api*).
