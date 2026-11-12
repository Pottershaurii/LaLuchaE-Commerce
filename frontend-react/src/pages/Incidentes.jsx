import { useEffect, useState } from 'react'
import { Alert, Badge, Button, Col, Container, Form, Row, Table } from 'react-bootstrap'

import api from '../services/api'
import { useAuth } from '../context/AuthContext'
import '../styles/incidentes.css'

// Gestión de incidentes (Semana 14 - Sesión 28):
// formulario para reportar un error y panel de tickets activos.

const FORMULARIO_VACIO = {
  titulo: '',
  modulo: '',
  descripcion: '',
  prioridad: 'P3'
}

const COLOR_PRIORIDAD = {
  P1: 'danger',
  P2: 'warning',
  P3: 'primary',
  P4: 'secondary'
}

const ACTUALIZAR_CADA_MS = 30000

function leerRol() {
  try {
    return JSON.parse(localStorage.getItem('lalucha_user'))?.rol || ''
  } catch {
    return ''
  }
}

function formatearFecha(iso) {
  if (!iso) return ''
  return new Date(iso).toLocaleString('es-PE', {
    dateStyle: 'short',
    timeStyle: 'short'
  })
}

function mensajeDeError(error, porDefecto) {
  const datos = error.response?.data
  if (datos?.errores) return Object.values(datos.errores).join(' ')
  return datos?.message || porDefecto
}

function Incidentes({ onLogin }) {
  const { isAuthenticated } = useAuth()

  const [catalogos, setCatalogos] = useState(null)
  const [tickets, setTickets] = useState([])
  const [formulario, setFormulario] = useState(FORMULARIO_VACIO)
  const [enviando, setEnviando] = useState(false)
  const [aviso, setAviso] = useState(null)
  const [errorCarga, setErrorCarga] = useState('')
  const [version, setVersion] = useState(0)

  const rol = isAuthenticated ? leerRol() : ''
  const esPersonal = rol === 'ROLE_ADMIN' || rol === 'ROLE_STAFF'

  // Carga catálogos y tickets activos; se repite al registrar o cambiar un ticket
  useEffect(() => {
    if (!isAuthenticated) return undefined

    let vigente = true

    Promise.all([api.get('/incidentes/catalogos'), api.get('/incidentes')])
      .then(([respuestaCatalogos, respuestaTickets]) => {
        if (!vigente) return
        setCatalogos(respuestaCatalogos.data)
        setTickets(respuestaTickets.data)
        setErrorCarga('')
      })
      .catch(() => {
        if (vigente) setErrorCarga('No se pudo conectar con el módulo de incidentes.')
      })

    return () => {
      vigente = false
    }
  }, [isAuthenticated, version])

  // Seguimiento en tiempo real: refresca el panel cada 30 segundos
  useEffect(() => {
    if (!isAuthenticated) return undefined
    const intervalo = setInterval(() => setVersion((v) => v + 1), ACTUALIZAR_CADA_MS)
    return () => clearInterval(intervalo)
  }, [isAuthenticated])

  const cambiarCampo = (e) => {
    const { name, value } = e.target
    setFormulario((actual) => ({ ...actual, [name]: value }))
  }

  const registrar = async (e) => {
    e.preventDefault()
    setEnviando(true)
    setAviso(null)

    try {
      const { data } = await api.post('/incidentes', formulario)
      setAviso({
        tipo: 'success',
        texto: `Ticket ${data.codigo} registrado el ${formatearFecha(data.fechaCreacion)} con prioridad ${data.prioridad} (${data.prioridadNombre}). Estado: ${data.estado}. Asignado a ${data.asignado}.`
      })
      setFormulario(FORMULARIO_VACIO)
      setVersion((v) => v + 1)
    } catch (error) {
      setAviso({
        tipo: 'danger',
        texto: mensajeDeError(error, 'No se pudo registrar el ticket.')
      })
    } finally {
      setEnviando(false)
    }
  }

  const actualizarTicket = async (id, cambio) => {
    try {
      await api.patch(`/incidentes/${id}`, cambio)
      setVersion((v) => v + 1)
    } catch (error) {
      setAviso({
        tipo: 'danger',
        texto: mensajeDeError(error, 'No se pudo actualizar el ticket.')
      })
    }
  }

  if (!isAuthenticated) {
    return (
      <div className="incidentes-section">
        <Container>
          <div className="incidentes-encabezado">
            <h2>Soporte</h2>
            <p>Inicia sesión para reportar un error o hacer seguimiento de tus tickets.</p>
            <Button className="btn-hero-primary" onClick={onLogin}>
              Iniciar sesión
            </Button>
          </div>
        </Container>
      </div>
    )
  }

  return (
    <div className="incidentes-section">
      <Container>
        <div className="incidentes-encabezado">
          <h2>Soporte: reporte de incidentes</h2>
          <p>
            Cada reporte genera un ticket con código, fecha y prioridad.
            {esPersonal
              ? ' Como personal, puedes cambiar el estado y escalar el ticket.'
              : ' Aquí puedes ver el estado de tus tickets.'}
          </p>
        </div>

        {errorCarga && <Alert variant="warning">{errorCarga}</Alert>}

        <Row className="g-4">
          {/* ================= FORMULARIO ================= */}
          <Col xs={12} lg={4}>
            <Form className="incidentes-card" onSubmit={registrar}>
              <h3>Nuevo reporte</h3>

              <Form.Group className="mb-3" controlId="incidente-titulo">
                <Form.Label>Título del error</Form.Label>
                <Form.Control
                  name="titulo"
                  value={formulario.titulo}
                  onChange={cambiarCampo}
                  maxLength={150}
                  placeholder="Ej. La pasarela de pagos no responde"
                  required
                />
              </Form.Group>

              <Form.Group className="mb-3" controlId="incidente-modulo">
                <Form.Label>Módulo afectado</Form.Label>
                <Form.Select
                  name="modulo"
                  value={formulario.modulo}
                  onChange={cambiarCampo}
                  required
                >
                  <option value="">Selecciona un módulo</option>
                  {catalogos?.modulos.map((modulo) => (
                    <option key={modulo} value={modulo}>{modulo}</option>
                  ))}
                </Form.Select>
              </Form.Group>

              <Form.Group className="mb-3" controlId="incidente-descripcion">
                <Form.Label>Descripción detallada</Form.Label>
                <Form.Control
                  as="textarea"
                  rows={4}
                  name="descripcion"
                  value={formulario.descripcion}
                  onChange={cambiarCampo}
                  maxLength={4000}
                  placeholder="Qué estabas haciendo, qué mensaje apareció y desde cuándo pasa"
                  required
                />
              </Form.Group>

              <Form.Group className="mb-3" controlId="incidente-prioridad">
                <Form.Label>Prioridad</Form.Label>
                <Form.Select
                  name="prioridad"
                  value={formulario.prioridad}
                  onChange={cambiarCampo}
                >
                  {catalogos?.prioridades.map((p) => (
                    <option key={p.codigo} value={p.codigo}>
                      {p.codigo} · {p.nombre} (resolver en {p.horasObjetivo} h)
                    </option>
                  ))}
                </Form.Select>
              </Form.Group>

              <Button type="submit" className="btn-hero-primary w-100" disabled={enviando || !catalogos}>
                {enviando ? 'Registrando…' : 'Registrar ticket'}
              </Button>

              {aviso && (
                <Alert variant={aviso.tipo} className="mt-3 mb-0">
                  {aviso.texto}
                </Alert>
              )}
            </Form>
          </Col>

          {/* ================= PANEL DE TICKETS ACTIVOS ================= */}
          <Col xs={12} lg={8}>
            <div className="incidentes-card">
              <div className="d-flex justify-content-between align-items-center mb-3">
                <h3 className="mb-0">Tickets activos ({tickets.length})</h3>
                <Button
                  variant="outline-secondary"
                  size="sm"
                  onClick={() => setVersion((v) => v + 1)}
                >
                  Actualizar
                </Button>
              </div>

              {tickets.length === 0 ? (
                <p className="text-muted mb-0">No hay tickets activos.</p>
              ) : (
                <Table responsive hover size="sm" className="incidentes-tabla mb-0">
                  <thead>
                    <tr>
                      <th>Código</th>
                      <th>Título</th>
                      <th>Prioridad</th>
                      <th>Estado</th>
                      <th>Asignado a</th>
                      <th>Creado</th>
                    </tr>
                  </thead>
                  <tbody>
                    {tickets.map((t) => (
                      <tr key={t.id}>
                        <td className="fw-semibold">{t.codigo}</td>
                        <td>
                          {t.titulo}
                          <div className="incidentes-detalle">{t.modulo} · {t.reportadoPor}</div>
                        </td>
                        <td>
                          <Badge bg={COLOR_PRIORIDAD[t.prioridad] || 'secondary'}>
                            {t.prioridad} · {t.prioridadNombre}
                          </Badge>
                        </td>
                        <td>
                          {esPersonal ? (
                            <Form.Select
                              size="sm"
                              aria-label={`Estado de ${t.codigo}`}
                              value={t.estadoId}
                              onChange={(e) => actualizarTicket(t.id, { estadoId: Number(e.target.value) })}
                            >
                              {catalogos?.estados.map((estado) => (
                                <option key={estado.id} value={estado.id}>{estado.nombre}</option>
                              ))}
                            </Form.Select>
                          ) : (
                            t.estado
                          )}
                        </td>
                        <td>
                          {esPersonal ? (
                            <Form.Select
                              size="sm"
                              aria-label={`Asignado de ${t.codigo}`}
                              value={t.asignadoId ?? ''}
                              onChange={(e) => actualizarTicket(t.id, { asignadoId: Number(e.target.value) })}
                            >
                              {catalogos?.asignados.map((grupo) => (
                                <option key={grupo.id} value={grupo.id}>
                                  N{grupo.nivel} · {grupo.nombre}
                                </option>
                              ))}
                            </Form.Select>
                          ) : (
                            t.asignado && `N${t.nivel} · ${t.asignado}`
                          )}
                        </td>
                        <td className="text-nowrap">{formatearFecha(t.fechaCreacion)}</td>
                      </tr>
                    ))}
                  </tbody>
                </Table>
              )}
            </div>
          </Col>
        </Row>
      </Container>
    </div>
  )
}

export default Incidentes
