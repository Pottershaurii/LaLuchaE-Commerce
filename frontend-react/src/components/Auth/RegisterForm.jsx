import { useState } from 'react';
import { Form, Button, Alert, Card, Spinner } from 'react-bootstrap';
import { registerClient } from '../../services/authService';

function RegisterForm({ onSuccess }) {
  const [formData, setFormData] = useState({ nombre: '', email: '', password: '', telefono: '' });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleChange = (e) => setFormData({ ...formData, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      await registerClient(formData);
      onSuccess(formData.email);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card className="shadow-lg border-0 p-4" style={{ backgroundColor: 'var(--color-primary-soft)', color: 'var(--color-light)' }}>
      <Card.Body>
        <h3 className="fw-bold mb-4 text-center" style={{ color: 'var(--color-gold)' }}>Crea tu Cuenta</h3>
        {error && <Alert variant="danger">{error}</Alert>}
        <Form onSubmit={handleSubmit}>
          <Form.Group className="mb-3">
            <Form.Label>Nombre Completo</Form.Label>
            <Form.Control type="text" name="nombre" placeholder="Ej. Juan Pérez" value={formData.nombre} onChange={handleChange} required />
          </Form.Group>
          <Form.Group className="mb-3">
            <Form.Label>Correo Electrónico</Form.Label>
            <Form.Control type="email" name="email" placeholder="correo@ejemplo.com" value={formData.email} onChange={handleChange} required />
          </Form.Group>
          <Form.Group className="mb-3">
            <Form.Label>Teléfono</Form.Label>
            <Form.Control type="tel" name="telefono" placeholder="987654321" value={formData.telefono} onChange={handleChange} required />
          </Form.Group>
          <Form.Group className="mb-4">
            <Form.Label>Contraseña</Form.Label>
            <Form.Control type="password" name="password" placeholder="Mínimo 6 caracteres" value={formData.password} onChange={handleChange} required minLength={6} />
          </Form.Group>
          <Button variant="primary" type="submit" className="w-100 py-2 fw-bold" disabled={loading}>
            {loading ? <Spinner animation="border" size="sm" /> : 'REGISTRARME'}
          </Button>
        </Form>
      </Card.Body>
    </Card>
  );
}

export default RegisterForm;