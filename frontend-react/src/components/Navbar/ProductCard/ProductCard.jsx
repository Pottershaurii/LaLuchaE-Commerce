import { Card, Button } from 'react-bootstrap'

function ProductCard({ imagen, nombre, descripcion, precio }) {
  return (
    <Card className="h-100 product-card">
      <Card.Img
        variant="top"
        src={imagen}
        alt={nombre}
        className="product-card-img"
      />

      <Card.Body className="d-flex flex-column">
        <Card.Title>{nombre}</Card.Title>

        <Card.Text className="flex-grow-1">
          {descripcion}
        </Card.Text>

        <div className="d-flex justify-content-between align-items-center mt-3">
          <span className="fw-bold fs-5">
            S/ {precio.toFixed(2)}
          </span>

          <Button className="btn-lalucha">
            Agregar
          </Button>
        </div>
      </Card.Body>
    </Card>
  )
}

export default ProductCard