import { useState } from 'react';
import { Card, Button, Form, Alert } from 'react-bootstrap';
import { verifyEmailToken } from '../../services/authService';

function EmailVerificationNotice({ email }) {
  const [token, setToken] = useState('');
  const [verified, setVerified] = useState(false);
  const [error, setError] = useState('');

  const handleVerify = async (e) => {
    e.preventDefault();
    setError('');
    try {
      await verifyEmailToken(token);
      setVerified(true);
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <Card className="shadow-lg border-0 p-4 text-center" style={{ backgroundColor: 'var(--color-primary-soft)', color: 'var(--color-light)' }}>
      <Card.Body>
        <h3 className="fw-bold mb-3" style={{ color: 'var(--color-gold)' }}>¡Casi listo!</h3>
        {!verified ? (
          <>
            <p className="mb-3">Hemos enviado un código a <strong>{email}</strong>.</p>
            <p className="text-muted small mb-4">Ingresa el código de prueba <strong>123456</strong>:</p>
            {error && <Alert variant="danger">{error}</Alert>}
            <Form onSubmit={handleVerify} className="d-flex gap-2 mb-3">
              <Form.Control type="text" placeholder="123456" value={token} onChange={(e) => setToken(e.target.value)} required />
              <Button variant="primary" type="submit">Validar</Button>
            </Form>
          </>
        ) : (
          <Alert variant="success" className="mb-0">
            <h5>¡Correo Validado Correctamente!</h5>
            <p className="mb-0">Tu cuenta ya está activa.</p>
          </Alert>
        )}
      </Card.Body>
    </Card>
  );
}

export default EmailVerificationNotice;