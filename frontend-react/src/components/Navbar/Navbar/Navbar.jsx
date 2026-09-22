import { Container, Nav, Navbar as BootstrapNavbar } from 'react-bootstrap'
import { Link } from 'react-router-dom'
import {
  LuSearch,
  LuUserRound,
  LuShoppingBag
} from 'react-icons/lu'

import logo from '../../../assets/logo.png'

function Navbar() {
  return (
    <BootstrapNavbar
      expand="lg"
      className="lalucha-navbar"
    >
      <Container fluid className="lalucha-navbar-container">

        {/* LOGO + NOMBRE */}
        <BootstrapNavbar.Brand
          as={Link}
          to="/"
          className="lalucha-brand"
        >
          <div className="lalucha-logo-wrapper">
            <img
              src={logo}
              alt="Logo La Lucha"
              className="lalucha-logo-img"
            />
          </div>

          <div className="lalucha-brand-info">
            <span className="lalucha-brand-name">
              LA LUCHA
            </span>

            <span className="lalucha-brand-description">
              SANGUCHERÍA CRIOLLA
            </span>
          </div>
        </BootstrapNavbar.Brand>


        {/* BOTÓN RESPONSIVE */}
        <BootstrapNavbar.Toggle
          aria-controls="lalucha-navbar-collapse"
          className="lalucha-navbar-toggle"
        />


        <BootstrapNavbar.Collapse
          id="lalucha-navbar-collapse"
          className="lalucha-navbar-collapse"
        >

          {/* MENÚ CENTRAL */}
          <Nav className="lalucha-menu">

            <Nav.Link
              as={Link}
              to="/"
              className="lalucha-menu-link lalucha-menu-active"
            >
              Inicio
            </Nav.Link>

            <Nav.Link
              href="/#catalogo"
              className="lalucha-menu-link"
            >
              Menú
            </Nav.Link>

            <Nav.Link
              href="/#promociones"
              className="lalucha-menu-link"
            >
              Promociones
            </Nav.Link>

            <Nav.Link
              href="/#nosotros"
              className="lalucha-menu-link"
            >
              Nosotros
            </Nav.Link>

            <Nav.Link
              href="/#contacto"
              className="lalucha-menu-link"
            >
              Contacto
            </Nav.Link>

          </Nav>


          {/* ICONOS DERECHA */}
          <div className="lalucha-actions">

            <button
              type="button"
              className="lalucha-action-search"
              aria-label="Buscar"
            >
              <LuSearch />
            </button>


            <Link
              to="/login"
              className="lalucha-action-login"
            >
              <LuUserRound />

              <span>
                Ingresar
              </span>
            </Link>


            <button
              type="button"
              className="lalucha-action-cart"
              aria-label="Carrito"
            >
              <LuShoppingBag />

              <span className="lalucha-cart-badge">
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