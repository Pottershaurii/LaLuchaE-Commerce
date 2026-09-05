import { useState } from 'react'
import { Button, Card, Col, Container, Row, Modal } from 'react-bootstrap'
import Navbar from './components/Navbar/Navbar'
import RegisterForm from './components/Auth/RegisterForm'
import EmailVerificationNotice from './components/Auth/EmailVerificationNotice'

function App() {
  // Estado para controlar la visibilidad del modal de Registro/Login
  const [showAuthModal, setShowAuthModal] = useState(false)
  
  // Estado para almacenar el email registrado y alternar a la pantalla de verificación
  const [registeredEmail, setRegisteredEmail] = useState(null)

  const handleCloseModal = () => {
    setShowAuthModal(false)
    setRegisteredEmail(null) // Reinicia el flujo al cerrar
  }

  const handleOpenModal = () => setShowAuthModal(true)

  return (
    <>
      {/* NAVBAR CON ACCIONES DE CUENTA */}
      <Navbar onOpenAuth={handleOpenModal} />

      <main>
        {/* HERO */}
        <section className="hero-section py-5">
          <Container className="py-5">
            <p className="text-uppercase fw-bold text-danger">
              La Lucha Sanguchería Criolla
            </p>

            <h1 className="display-3 fw-bold">
              Sánguches Criollos
            </h1>

            <p className="lead mb-4">
              El sabor que nos representa.
            </p>

            <div className="d-flex gap-3">
              <Button variant="primary" href="#productos">
                Ver carta
              </Button>
              <Button variant="outline-light" onClick={handleOpenModal}>
                Crear Cuenta
              </Button>
            </div>
          </Container>
        </section>

        {/* PRODUCTOS */}
        <section id="productos" className="py-5">
          <Container>
            <h2 className="section-title mb-4">
              Nuestros favoritos
            </h2>

            <Row className="g-4">
              <Col xs={12} md={6} lg={4}>
                <Card className="h-100 shadow-sm">
                  <Card.Body>
                    <Card.Title>Sánguche de Chicharrón</Card.Title>
                    <Card.Text>Un clásico de La Lucha.</Card.Text>
                    <Button variant="primary">Agregar</Button>
                  </Card.Body>
                </Card>
              </Col>

              <Col xs={12} md={6} lg={4}>
                <Card className="h-100 shadow-sm">
                  <Card.Body>
                    <Card.Title>Sánguche de Pollo</Card.Title>
                    <Card.Text>Preparado con ingredientes seleccionados.</Card.Text>
                    <Button variant="primary">Agregar</Button>
                  </Card.Body>
                </Card>
              </Col>

              <Col xs={12} md={6} lg={4}>
                <Card className="h-100 shadow-sm">
                  <Card.Body>
                    <Card.Title>Jugo Natural</Card.Title>
                    <Card.Text>El acompañamiento ideal.</Card.Text>
                    <Button variant="primary">Agregar</Button>
                  </Card.Body>
                </Card>
              </Col>
            </Row>
          </Container>
        </section>
      </main>

      {/* MODAL HU02: REGISTRO Y VALIDACIÓN DE CORREO */}
      <Modal 
        show={showAuthModal} 
        onHide={handleCloseModal} 
        centered 
        backdrop="static"
        contentClassName="bg-transparent border-0"
      >
        <Modal.Header closeButton closeVariant="white" className="border-0 pb-0" style={{ backgroundColor: 'var(--color-primary-soft)' }} />
        <Modal.Body className="p-0">
          {!registeredEmail ? (
            <RegisterForm onSuccess={(email) => setRegisteredEmail(email)} />
          ) : (
            <EmailVerificationNotice email={registeredEmail} />
          )}
        </Modal.Body>
      </Modal>
    </>
  )
}

export default App