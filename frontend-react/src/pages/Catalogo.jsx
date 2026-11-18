import { Container, Row, Col } from 'react-bootstrap'
import ProductCard from '../components/Navbar/ProductCard/ProductCard.jsx'
import { useEffect, useState } from 'react'

const API = import.meta.env.VITE_API_URL || 'http://localhost:8080'

function Catalogo() {

  const [productos, setProductos] = useState([])

  useEffect(() => {
    fetch(`${API}/api/productos`)
      .then(r => r.json())
      .then(data => {
        const mapeados = data.map(p => ({
          id: p.id,
          nombre: p.nombre,
          descripcion: p.descripcion,
          precio: p.precio,
          imagen: p.imagenUrl || '/placeholder-producto.svg'
        }))
        setProductos(mapeados)
      })
      .catch(() => setProductos([]))
  }, [])

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
