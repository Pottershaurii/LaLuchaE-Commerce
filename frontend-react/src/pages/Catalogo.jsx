import { Container, Row, Col } from 'react-bootstrap'
import ProductCard from '../components/Navbar/ProductCard/ProductCard.jsx'
import lechonImg from '../assets/images/lechon.jfif'
import pavoImg from '../assets/images/pavo.jfif'
import chicharronImg from '../assets/images/chicharron.jpg'
import laLuchaImg from '../assets/images/la-lucha.jfif'
import asadoImg from '../assets/images/asado.jfif'
import hamburguesaImg from '../assets/images/hamburguesa.jpeg'

function Catalogo() {

  const productos = [
    {
      id: 1,
      nombre: 'Lechón a la Leña',
      descripcion: 'Sánguche de lechón a la leña.',
      precio: 14.60,
      imagen: lechonImg,
    },
    {
      id: 2,
      nombre: 'Pavo a la Leña',
      descripcion: 'Sánguche de pavo preparado a la leña.',
      precio: 14.90,
      imagen: pavoImg,
    },
    {
      id: 3,
      nombre: 'Chicharrón',
      descripcion: 'Sánguche de chicharrón.',
      precio: 13.90,
      imagen: chicharronImg,
    },
    {
      id: 4,
      nombre: 'La Lucha',
      descripcion: 'Sánguche especial de la casa.',
      precio: 15.90,
      imagen: laLuchaImg,
    },
    {
      id: 5,
      nombre: 'Asado de Res',
      descripcion: 'Sánguche de asado de res.',
      precio: 14.10,
      imagen: asadoImg,
    },
    {
      id: 6,
      nombre: 'Hamburguesa',
      descripcion: 'Hamburguesa de la casa.',
      precio: 13.90,
      imagen: hamburguesaImg,
    },
    {
      id: 7,
      nombre: 'Pollo Deshilachado',
      descripcion: 'Sánguche de pollo deshilachado.',
      precio: 12.60,
      imagen: '/placeholder-producto.jpg',
    },
    {
      id: 8,
      nombre: 'Pollo con Piña',
      descripcion: 'Sánguche de pollo acompañado con piña.',
      precio: 14.90,
      imagen: '/placeholder-producto.jpg',
    },
  ]

  return (
    <main className="catalogo py-5">
      <Container>

        <div className="text-center mb-5">
          <p className="catalogo-subtitulo">
            LA LUCHA SANGUCHERÍA CRIOLLA
          </p>

          <h1>Nuestra Carta</h1>

          <p>
            Disfruta nuestros sánguches preparados al estilo de La Lucha.
          </p>
        </div>

        <div className="text-center mb-5">
          <button className="btn btn-lalucha me-2">
            Todos
          </button>

          <button className="btn btn-outline-secondary me-2">
            Sánguches
          </button>

          <button className="btn btn-outline-secondary me-2">
            Jugos
          </button>

          <button className="btn btn-outline-secondary">
            Bebidas
          </button>
        </div>

        <h2 className="mb-4">Sánguches</h2>

        <Row className="g-4">
          {productos.map((producto) => (
            <Col
              key={producto.id}
              xs={12}
              sm={6}
              lg={4}
              xl={3}
            >
              <ProductCard
                imagen={producto.imagen}
                nombre={producto.nombre}
                descripcion={producto.descripcion}
                precio={producto.precio}
              />
            </Col>
          ))}
        </Row>

      </Container>
    </main>
  )
}

export default Catalogo