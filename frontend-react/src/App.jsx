import { Button, Col, Container, Row } from 'react-bootstrap'

import {
  LuSandwich,
  LuUtensils,
  LuCupSoda,
  LuIceCreamCone
} from 'react-icons/lu'

import { PiDrop } from 'react-icons/pi'

import Navbar from './components/Navbar/Navbar/Navbar.jsx'
import Catalogo from './pages/Catalogo.jsx'
import heroImg from './assets/images/hero-sanguche.jpg'


function App() {
  return (
    <>
      {/* ================= NAVBAR ================= */}
      <Navbar />


      <main>

        {/* ================= HERO ================= */}
        <section className="hero-section">

          <Container>

            <Row className="align-items-center g-5">

              {/* ---------- TEXTO ---------- */}
              <Col xs={12} lg={6}>

                <span className="hero-badge">
                  ESPECIALIDAD LA LUCHA
                </span>


                <h1 className="hero-title">
                  SÁNGUCHES
                  <br />
                  CRIOLLOS
                </h1>


                <h2 className="hero-script">
                  Hechos como en casa
                </h2>


                <p className="hero-description">
                  Tradición criolla horneada diariamente, carnes jugosas
                  seleccionadas y la infaltable sarza criolla.
                </p>


                <div className="d-flex flex-wrap gap-3">

                  <Button
                    href="#catalogo"
                    className="btn-hero-primary"
                  >
                    Ver menú
                  </Button>


                  <Button
                    href="#promociones"
                    className="btn-hero-secondary"
                  >
                    Promociones del día
                  </Button>

                </div>

              </Col>


              {/* ---------- IMAGEN ---------- */}
              <Col xs={12} lg={6}>

                <div className="hero-image-container">

                  <img
                    src={heroImg}
                    alt="Sánguche criollo La Lucha"
                    className="hero-image"
                  />


                  {/* FLECHA IZQUIERDA */}
                  <button
                    type="button"
                    className="hero-arrow hero-arrow-left"
                    aria-label="Imagen anterior"
                  >
                    ‹
                  </button>


                  {/* FLECHA DERECHA */}
                  <button
                    type="button"
                    className="hero-arrow hero-arrow-right"
                    aria-label="Imagen siguiente"
                  >
                    ›
                  </button>

                </div>


                {/* INDICADORES DEL CARRUSEL */}
                <div className="hero-dots">

                  <span className="hero-dot active"></span>

                  <span className="hero-dot"></span>

                  <span className="hero-dot"></span>

                </div>

              </Col>

            </Row>

          </Container>

        </section>



        {/* ================= CATEGORÍAS ================= */}
        <section className="categorias-section">

          <Container>

            {/* TÍTULO */}
            <div className="categorias-header">

              <h2>
                Categorías
              </h2>

              <a href="#catalogo">
                Ver todas
              </a>

            </div>


            {/* LISTA DE CATEGORÍAS */}
            <div className="categorias-list">


              {/* SÁNGUCHES */}
              <a
                href="#catalogo"
                className="categoria-item active"
              >

                <div className="categoria-icon">
                  <LuSandwich />
                </div>

                <span>
                  Sánguches
                </span>

              </a>



              {/* COMBOS */}
              <a
                href="#catalogo"
                className="categoria-item"
              >

                <div className="categoria-icon">
                  <LuUtensils />
                </div>

                <span>
                  Combos
                </span>

              </a>



              {/* BEBIDAS */}
              <a
                href="#catalogo"
                className="categoria-item"
              >

                <div className="categoria-icon">
                  <LuCupSoda />
                </div>

                <span>
                  Bebidas
                </span>

              </a>



              {/* EXTRAS */}
              <a
                href="#catalogo"
                className="categoria-item"
              >

                <div className="categoria-icon">
                  <PiDrop />
                </div>

                <span>
                  Extras
                </span>

              </a>



              {/* POSTRES */}
              <a
                href="#catalogo"
                className="categoria-item"
              >

                <div className="categoria-icon">
                  <LuIceCreamCone />
                </div>

                <span>
                  Postres
                </span>

              </a>

            </div>

          </Container>

        </section>



        {/* ================= CATÁLOGO ================= */}
        <section id="catalogo">

          <Catalogo />

        </section>

      </main>
    </>
  )
}


export default App