import { useState } from 'react'

import { registerClient } from '../../services/authService'

import '../../styles/register.css'

import logo from '../../assets/images/logo.png'

function RegisterForm({ onSuccess, onClose, onLogin }) {
  const [formData, setFormData] = useState({
    nombre: '',
    email: '',
    password: ''
  })

  const [mostrarPassword, setMostrarPassword] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleChange = (e) => {
    const { name, value } = e.target

    setFormData((prev) => ({
      ...prev,
      [name]: value
    }))
  }

  const handleSubmit = async (e) => {
    e.preventDefault()

    setError('')
    setLoading(true)

    try {
      await registerClient(formData)

      if (onSuccess) {
        onSuccess(formData.email)
      }
    } catch (err) {
      setError(
        err.message ||
        'No se pudo completar el registro.'
      )
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="register-layout">
      <button
        type="button"
        className="register-modal-close"
        onClick={onClose}
        aria-label="Cerrar registro"
      >
        ×
      </button>

      {/* ================= PANEL IZQUIERDO ================= */}

      <div className="register-brand-panel">
        <div className="register-brand-content">
          <div className="register-main-logo">
            <img
              src={logo}
              alt="La Lucha Sanguchería Criolla"
            />
          </div>

          <h2>
            SABOR QUE NOS UNE
          </h2>

          <p>
            Criollo de corazón
          </p>
        </div>

        <span className="register-copyright">
          LA LUCHA SANGUCHERÍA CRIOLLA © 2026
        </span>
      </div>

      {/* ================= FORMULARIO ================= */}

      <div className="register-form-panel">
        <div className="register-small-logo">
          <img
            src={logo}
            alt="La Lucha"
          />
        </div>

        <h1>
          Crear una cuenta
        </h1>

        <p className="register-subtitle">
          Completa tus datos para disfrutar promociones exclusivas
        </p>

        <form onSubmit={handleSubmit}>
          {/* NOMBRE */}

          <div className="register-field">
            <label htmlFor="nombre">
              Nombre completo
            </label>

            <div className="register-input">
              <input
                id="nombre"
                name="nombre"
                type="text"
                placeholder="Ej. Juan Pérez"
                value={formData.nombre}
                onChange={handleChange}
                autoComplete="name"
                required
              />
            </div>
          </div>

          {/* CORREO */}

          <div className="register-field">
            <label htmlFor="email">
              Correo electrónico
            </label>

            <div className="register-input">
              <span className="register-input-icon">
                ✉
              </span>

              <input
                id="email"
                name="email"
                type="email"
                placeholder="correo@ejemplo.com"
                value={formData.email}
                onChange={handleChange}
                autoComplete="email"
                required
              />
            </div>
          </div>

          {/* CONTRASEÑA */}

          <div className="register-field">
            <label htmlFor="password">
              Contraseña
            </label>

            <div className="register-input">
              <span className="register-input-icon">
                ♙
              </span>

              <input
                id="password"
                name="password"
                type={
                  mostrarPassword
                    ? 'text'
                    : 'password'
                }
                placeholder="••••••••"
                value={formData.password}
                onChange={handleChange}
                autoComplete="new-password"
                minLength="6"
                required
              />

              <button
                type="button"
                className="register-password-toggle"
                onClick={() =>
                  setMostrarPassword(
                    !mostrarPassword
                  )
                }
                aria-label={
                  mostrarPassword
                    ? 'Ocultar contraseña'
                    : 'Mostrar contraseña'
                }
              >
                {mostrarPassword ? '◉' : '◎'}
              </button>
            </div>
          </div>

          {/* ERROR */}

          {error && (
            <div className="register-error">
              {error}
            </div>
          )}

          {/* BOTÓN */}

          <button
            type="submit"
            className="register-submit"
            disabled={loading}
          >
            {loading
              ? 'Registrando...'
              : 'Registrarme'}
          </button>
        </form>

        {/* ================= SOCIAL ================= */}

        <div className="register-separator">
          <span>
            O CONTINÚA CON
          </span>
        </div>

        <div className="register-social">
          <button
            type="button"
            className="register-google"
          >
            <strong>G</strong>
            Google
          </button>

          <button
            type="button"
            className="register-facebook"
          >
            <strong>f</strong>
            Facebook
          </button>
        </div>

        {/* ================= LOGIN ================= */}

        <p className="register-login-text">
          ¿Ya tienes una cuenta?{' '}

          <button
            type="button"
            onClick={onLogin}
          >
            Inicia sesión aquí
          </button>
        </p>
      </div>
    </div>
  )
}

export default RegisterForm
