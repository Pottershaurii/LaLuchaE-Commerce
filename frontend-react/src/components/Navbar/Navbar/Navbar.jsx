import { Container, Nav, Navbar as BootstrapNavbar } from 'react-bootstrap'

function Navbar() {
  return (
    <BootstrapNavbar expand="lg" bg="light" className="py-3 shadow-sm">
      <Container>

        <BootstrapNavbar.Brand
          href="#"
          className="fw-bold fs-3"
        >
          LA LUCHA
        </BootstrapNavbar.Brand>

        <BootstrapNavbar.Toggle />

        <BootstrapNavbar.Collapse>
          <Nav className="ms-auto fw-semibold">

            <Nav.Link href="#">
              Inicio
            </Nav.Link>

            <Nav.Link href="#productos">
              Carta
            </Nav.Link>

            <Nav.Link href="#">
              Locales
            </Nav.Link>

            <Nav.Link href="#">
              Carrito
            </Nav.Link>

          </Nav>
        </BootstrapNavbar.Collapse>

      </Container>
    </BootstrapNavbar>
  )
}

export default Navbar