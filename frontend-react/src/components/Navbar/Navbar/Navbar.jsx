import {
  Container,
  Nav,
  Navbar as BootstrapNavbar,
  Button
} from 'react-bootstrap'

function Navbar({ onOpenAuth }) {
  return (
    <BootstrapNavbar
      expand="lg"
      className="lalucha-navbar"
    >
      <Container>
        <BootstrapNavbar.Brand
          href="#"
          className="fw-bold fs-3"
        >
          LA LUCHA
        </BootstrapNavbar.Brand>

        <BootstrapNavbar.Toggle
          aria-controls="lalucha-navbar"
        />

        <BootstrapNavbar.Collapse id="lalucha-navbar">
          <Nav className="ms-auto align-items-lg-center fw-semibold gap-lg-3">

            <Nav.Link href="#">
              Inicio
            </Nav.Link>

            <Nav.Link href="#catalogo">
              Carta
            </Nav.Link>

            <Nav.Link href="#">
              Locales
            </Nav.Link>

            <Nav.Link href="#">
              Carrito
            </Nav.Link>

            <Button
              type="button"
              variant="outline-warning"
              className="ms-lg-2"
              onClick={onOpenAuth}
            >
              Registrarse
            </Button>

          </Nav>
        </BootstrapNavbar.Collapse>
      </Container>
    </BootstrapNavbar>
  )
}

export default Navbar