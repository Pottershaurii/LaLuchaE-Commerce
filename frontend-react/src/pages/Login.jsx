import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import '../styles/login.css'

import logo from '../assets/logo.png'

function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()

  const [correo, setCorreo] = useState('')
  const [password, setPassword] = useState('')
  const [mostrarPassword, setMostrarPassword] = useState(false)
  const [recordarme, setRecordarme] = useState(true)
  const [mensaje, setMensaje] = useState('')
  const [cargando, setCargando] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()

    setMensaje('')
    setCargando(true)

    try {
      await login(correo, password)
      navigate('/')
    } catch (error) {
      console.error('Error al iniciar sesión:', error)

      if (error.response?.status === 401) {
        setMensaje('Correo o contraseña incorrectos')
      } else {
        setMensaje('No se pudo conectar con el servidor')
      }
    } finally {
      setCargando(false)
    }
  }

  return (
    <div className="login-overlay">
      <div className="login-modal">

        {/* =========================
            PANEL IZQUIERDO
        ========================== */}
        <div className="login-brand-panel">

          <div className="login-brand-content">

            {/* LOGO GRANDE */}
            <div className="login-main-logo">
              <img
                src={logo}
                alt="La Lucha Sanguchería Criolla"
                className="login-main-logo-img"
              />
            </div>

            <h2>
              SABOR QUE NOS UNE
            </h2>

            <p>
              Criollo de corazón
            </p>

          </div>

          <span className="login-copyright">
            LA LUCHA SANGUCHERÍA CRIOLLA © 2026
          </span>

        </div>


        {/* =========================
            PANEL DERECHO
        ========================== */}
        <div className="login-form-panel">

          {/* CERRAR LOGIN */}
          <Link
            to="/"
            className="login-close"
            aria-label="Cerrar login"
          >
            ×
          </Link>


          {/* LOGO PEQUEÑO */}
          <div className="login-small-logo">
            <img
              src={logo}
              alt="La Lucha"
              className="login-small-logo-img"
            />
          </div>


          <h1>
            Bienvenido a La Lucha Sanguchería
          </h1>

          <p className="login-subtitle">
            Inicia sesión para continuar
          </p>


          {/* =========================
              FORMULARIO
          ========================== */}
          <form onSubmit={handleSubmit}>

            {/* CORREO */}
            <div className="login-field">

              <label htmlFor="correo">
                Correo electrónico
              </label>

              <div className="login-input">

                <span className="login-input-icon">
                  ✉
                </span>

                <input
                  id="correo"
                  type="email"
                  placeholder="correo@ejemplo.com"
                  value={correo}
                  onChange={(e) => setCorreo(e.target.value)}
                  autoComplete="email"
                  required
                />

              </div>

            </div>


            {/* CONTRASEÑA */}
            <div className="login-field">

              <label htmlFor="password">
                Contraseña
              </label>

              <div className="login-input">

                <span className="login-input-icon">
                  ♙
                </span>

                <input
                  id="password"
                  type={mostrarPassword ? 'text' : 'password'}
                  placeholder="••••••••"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  autoComplete="current-password"
                  required
                />

                <button
                  type="button"
                  className="password-toggle"
                  onClick={() =>
                    setMostrarPassword(!mostrarPassword)
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


            {/* RECORDAR / OLVIDÉ CONTRASEÑA */}
            <div className="login-options">

              <label className="remember-option">

                <input
                  type="checkbox"
                  checked={recordarme}
                  onChange={(e) =>
                    setRecordarme(e.target.checked)
                  }
                />

                <span>
                  Recordarme
                </span>

              </label>


              <button
                type="button"
                className="forgot-password"
              >
                ¿Olvidaste tu contraseña?
              </button>

            </div>


            {/* MENSAJE DE ERROR */}
            {mensaje && (
              <div className="login-message">
                {mensaje}
              </div>
            )}


            {/* BOTÓN LOGIN */}
            <button
              type="submit"
              className="login-submit"
              disabled={cargando}
            >
              {cargando
                ? 'Iniciando sesión...'
                : 'Iniciar sesión'}
            </button>

          </form>


          {/* =========================
              SEPARADOR
          ========================== */}
          <div className="login-separator">
            <span>
              O CONTINÚA CON
            </span>
          </div>


          {/* =========================
              REDES SOCIALES
          ========================== */}
          <div className="social-login">

            <button
              type="button"
              className="google-button"
            >
              <strong>G</strong>
              Google
            </button>


            <button
              type="button"
              className="facebook-button"
            >
              <strong>f</strong>
              Facebook
            </button>

          </div>


          {/* =========================
              REGISTRO
          ========================== */}
          <p className="register-text">
            ¿No tienes una cuenta?{' '}

            <button type="button">
              Regístrate aquí
            </button>
          </p>

        </div>
      </div>
    </div>
  )
}

export default Login