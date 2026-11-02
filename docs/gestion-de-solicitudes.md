# Gestion de Solicitudes de Servicio

**Proyecto:** La Lucha E-commerce · **Equipo:** Grupo 3 · **Curso:** Integrador II: Sistemas (UTP)

Una solicitud de servicio es un pedido de algo que ya esta previsto en el servicio (un dato, un acceso, un recurso)
y que no corresponde a una falla. Este documento define que solicitudes atiende el equipo, como se registran y
quien las aprueba, segun ITIL 4 (Service Request Management) e ISO/IEC 20000-1 (8.4.3).

---

## 1. Catalogo de solicitudes

| Codigo | Servicio | Tipo | Quien la pide | Aprobacion |
|---|---|---|---|---|
| REQ-INFO-01 | Consulta de un cliente sobre pedidos, productos o atencion | Informacion | Cliente registrado | No (preaprobada) |
| REQ-INFO-02 | Consulta tecnica interna (planes de Azure, configuracion, metricas) | Informacion | Integrante del equipo | No (preaprobada) |
| REQ-ACC-01 | Rol de personal (STAFF o ADMIN) en el panel de la tienda | Acceso | Trabajador del negocio | Si: gerente o dueno (Data Owner) |
| REQ-ACC-02 | Alta de un producto o combo con su receta de insumos | Provision de recursos | Jefe de cocina o de local | Si: gerente o dueno |
| REQ-ACC-03 | Acceso a infraestructura del equipo (Azure, GitHub, GHCR) | Acceso | Integrante del equipo | Si: administrador del recurso |

Las solicitudes de clientes y trabajadores (REQ-INFO-01, REQ-ACC-01 y REQ-ACC-02) se atenderan desde la tienda web
cuando se implemente el modulo de solicitudes (RFC-02, historias S5-HU-01 a S5-HU-03). Mientras tanto, y para las
solicitudes internas del equipo, la herramienta es **GitHub Issues** con formularios.

## 2. Herramientas

| Herramienta | Uso |
|---|---|
| GitHub Issues · formulario *Solicitud de acceso* | Registra REQ-ACC con recurso, nivel, justificacion, vigencia y aprobador |
| GitHub Issues · formulario *Solicitud de informacion* | Registra REQ-INFO con categoria, consulta y fecha limite |
| Etiquetas `solicitud`, `solicitud: acceso`, `solicitud: informacion` | Separan las solicitudes de los RFC y de las historias de usuario |
| Tablero Scrum (GitHub Projects) | Muestra el estado: Backlog, En progreso, Hecho |
| Historial del issue | Registro inmutable de quien pidio, quien aprobo, quien ejecuto y cuando |

## 3. Procedimiento

1. **Registro:** el solicitante llena el formulario. GitHub asigna el numero del issue, que sirve como numero de ticket.
2. **Validacion:** el responsable revisa que los campos esten completos, que el nivel pedido sea el minimo necesario
   y que no exista otra solicitud abierta para lo mismo. Si falta algo, lo pide en un comentario.
3. **Aprobacion:** solo para REQ-ACC. El aprobador (Data Owner del recurso) comenta *Aprobado* o *Rechazado* con el
   motivo. Nadie aprueba su propia solicitud (SoD).
4. **Ejecucion:** quien administra el recurso otorga el acceso o entrega la informacion. Nunca se publican
   contrasenas ni claves en el issue: los secretos se entregan por el gestor de Azure o por GitHub Secrets.
5. **Cierre:** el solicitante confirma que tiene lo que pidio y se cierra el issue. Los accesos con fecha de fin
   se revisan al cierre de cada sprint y se retiran si ya no se usan.

## 4. Acuerdos de nivel de servicio (horas laborables, lunes a sabado de 9:00 a 18:00)

| Codigo | Prioridad | Primera respuesta | Resolucion |
|---|---|---|---|
| REQ-INFO-01 / REQ-INFO-02 | Baja | 4 horas | 16 horas |
| REQ-ACC-01 / REQ-ACC-03 | Media | 2 horas | 8 horas |
| REQ-ACC-02 | Alta | 1 hora | 4 horas |

Los tiempos se miden con las fechas del historial del issue (creacion, primer comentario y cierre).
