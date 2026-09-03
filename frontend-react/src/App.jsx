import { Button, Card, Col, Container, Row } from 'react-bootstrap'
import Navbar from './components/Navbar/Navbar'

function App() {
  return (
    <>
      <Navbar />

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

            <Button variant="primary">
              Ver carta
            </Button>

          </Container>
        </section>


        {/* PRODUCTOS */}
        <section
          id="productos"
          className="py-5"
        >

          <Container>

            <h2 className="section-title">
              Nuestros favoritos
            </h2>

            <Row className="g-4">

              <Col xs={12} md={6} lg={4}>
                <Card className="h-100 shadow-sm">
                  <Card.Body>
                    <Card.Title>
                      Sánguche de Chicharrón
                    </Card.Title>

                    <Card.Text>
                      Un clásico de La Lucha.
                    </Card.Text>

                    <Button variant="primary">
                      Agregar
                    </Button>
                  </Card.Body>
                </Card>
              </Col>


              <Col xs={12} md={6} lg={4}>
                <Card className="h-100 shadow-sm">
                  <Card.Body>
                    <Card.Title>
                      Sánguche de Pollo
                    </Card.Title>

                    <Card.Text>
                      Preparado con ingredientes seleccionados.
                    </Card.Text>

                    <Button variant="primary">
                      Agregar
                    </Button>
                  </Card.Body>
                </Card>
              </Col>


              <Col xs={12} md={6} lg={4}>
                <Card className="h-100 shadow-sm">
                  <Card.Body>
                    <Card.Title>
                      Jugo Natural
                    </Card.Title>

                    <Card.Text>
                      El acompañamiento ideal.
                    </Card.Text>

                    <Button variant="primary">
                      Agregar
                    </Button>
                  </Card.Body>
                </Card>
              </Col>

            </Row>

          </Container>

        </section>

      </main>
    </>
  )
}

export default App