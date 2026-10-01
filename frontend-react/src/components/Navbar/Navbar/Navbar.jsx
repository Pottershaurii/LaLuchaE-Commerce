import {
  Container,
  Nav,
  Navbar as BootstrapNavbar
} from 'react-bootstrap'

import {
  LuSearch,
  LuUserRound,
  LuShoppingBag
} from 'react-icons/lu'

import logo from '../../../assets/images/logo.png'

function Navbar({ onOpenAuth }) {
  return (
    <BootstrapNavbar
      expand="lg"
      className="lalucha-navbar"
    >
      <Container className="navbar-container">

        {/* ================= MARCA ================= */}
        <BootstrapNavbar.Brand
          href="#inicio"
          className="lalucha-brand"
        >
          <img
            src={logo}
            alt="La Lucha Sanguchería Criolla"
            className="navbar-logo-img"
          />

          <div className="navbar-brand-text">
            <span className="navbar-brand-title">
              LA LUCHA
            </span>

            <span className="navbar-brand-subtitle">
              SANGUCHERÍA CRIOLLA
            </span>
          </div>
        </BootstrapNavbar.Brand>

        {/* ================= RESPONSIVE ================= */}
        <BootstrapNavbar.Toggle
          aria-controls="lalucha-navbar"
          className="navbar-toggle"
        />

        <BootstrapNavbar.Collapse id="lalucha-navbar">

          {/* ================= MENÚ ================= */}
          <Nav className="navbar-menu mx-auto">

            <Nav.Link
              href="#inicio"
              className="navbar-link active"
            >
              Inicio
            </Nav.Link>

            <Nav.Link
              href="#catalogo"
              className="navbar-link"
            >
              Menú
            </Nav.Link>

            <Nav.Link
              href="#promociones"
              className="navbar-link"
            >
              Promociones
            </Nav.Link>

            <Nav.Link
              href="#nosotros"
              className="navbar-link"
            >
              Nosotros
            </Nav.Link>

            <Nav.Link
              href="#contacto"
              className="navbar-link"
            >
              Contacto
            </Nav.Link>

          </Nav>

          {/* ================= ACCIONES ================= */}
          <div className="navbar-actions">

            <button
              type="button"
              className="navbar-icon-button"
              aria-label="Buscar"
            >
              <LuSearch />
            </button>

            <button
              type="button"
              className="navbar-login-button"
              onClick={onOpenAuth}
            >
              <LuUserRound className="navbar-user-icon" />

              <span>
                Ingresar
              </span>
            </button>

            <button
              type="button"
              className="navbar-cart-button"
              aria-label="Carrito"
            >
              <LuShoppingBag />

              <span className="navbar-cart-count">
                2
              </span>
            </button>

          </div>

        </BootstrapNavbar.Collapse>

      </Container>
    </BootstrapNavbar>
  )
}

export default Navbar