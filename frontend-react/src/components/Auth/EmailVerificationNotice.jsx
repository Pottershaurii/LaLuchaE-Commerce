import { useState } from 'react'

import {
  LuMail,
  LuCircleCheck,
  LuArrowLeft
} from 'react-icons/lu'

import { verifyEmailToken } from '../../services/authService'

import logo from '../../assets/images/logo.png'

function EmailVerificationNotice({
  email,
  onVerified
}) {
  const [token, setToken] = useState('')
  const [verified, setVerified] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleVerify = async (e) => {
    e.preventDefault()

    setError('')
    setLoading(true)

    try {
      await verifyEmailToken(token)
      setVerified(true)
    } catch (err) {
      setError(
        err.message ||
        'El código ingresado no es válido.'
      )
    } finally {
      setLoading(false)
    }
  }

  const handleTokenChange = (e) => {
    const value = e.target.value
      .replace(/\D/g, '')
      .slice(0, 6)

    setToken(value)
  }

  // =========================
  // CORREO VERIFICADO
  // =========================

  if (verified) {
    return (
      <div
        style={{
          width: '100%',
          maxWidth: '460px',
          padding: '45px 40px',
          textAlign: 'center',
          background: '#f8f4eb',
          borderRadius: '22px'
        }}
      >
        <img
          src={logo}
          alt="La Lucha"
          style={{
            width: '64px',
            height: '64px',
            maxWidth: '64px',
            maxHeight: '64px',
            objectFit: 'contain',
            display: 'block',
            margin: '0 auto 20px'
          }}
        />

        <div
          style={{
            fontSize: '58px',
            color: '#d79328',
            marginBottom: '15px'
          }}
        >
          <LuCircleCheck />
        </div>

        <h2
          style={{
            marginBottom: '12px',
            color: '#1b1916',
            fontWeight: '800'
          }}
        >
          ¡Correo verificado!
        </h2>

        <p
          style={{
            color: '#716b62',
            marginBottom: '8px'
          }}
        >
          Tu cuenta ha sido activada correctamente.
        </p>

        <p
          style={{
            color: '#a45a1e',
            fontWeight: '700',
            marginBottom: '28px'
          }}
        >
          {email}
        </p>

        <button
          type="button"
          onClick={onVerified}
          style={{
            width: '100%',
            height: '48px',
            border: 'none',
            borderRadius: '8px',
            background: '#1b1916',
            color: '#ffffff',
            fontWeight: '700',
            cursor: 'pointer'
          }}
        >
          Iniciar sesión
        </button>
      </div>
    )
  }

  // =========================
  // VERIFICACIÓN
  // =========================

  return (
    <div
      style={{
        width: '100%',
        maxWidth: '460px',
        padding: '42px 40px',
        textAlign: 'center',
        background: '#f8f4eb',
        borderRadius: '22px'
      }}
    >
      {/* LOGO */}

      <img
        src={logo}
        alt="La Lucha"
        style={{
          width: '64px',
          height: '64px',
          maxWidth: '64px',
          maxHeight: '64px',
          objectFit: 'contain',
          display: 'block',
          margin: '0 auto 18px'
        }}
      />

      {/* ICONO CORREO */}

      <div
        style={{
          width: '65px',
          height: '65px',
          margin: '0 auto 18px',
          borderRadius: '50%',
          background: '#f2dfb8',
          color: '#b87317',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          fontSize: '30px'
        }}
      >
        <LuMail />
      </div>

      <h2
        style={{
          margin: '0 0 10px',
          color: '#1b1916',
          fontSize: '28px',
          fontWeight: '800'
        }}
      >
        Verifica tu correo
      </h2>

      <p
        style={{
          margin: '0 0 5px',
          color: '#716b62',
          fontSize: '14px'
        }}
      >
        Hemos enviado un código de verificación a:
      </p>

      <p
        style={{
          marginBottom: '25px',
          color: '#a45a1e',
          fontWeight: '700'
        }}
      >
        {email}
      </p>

      <form onSubmit={handleVerify}>

        <label
          htmlFor="verification-code"
          style={{
            display: 'block',
            marginBottom: '8px',
            color: '#37322c',
            fontSize: '13px',
            fontWeight: '600',
            textAlign: 'left'
          }}
        >
          Código de verificación
        </label>

        <input
          id="verification-code"
          type="text"
          inputMode="numeric"
          autoComplete="one-time-code"
          placeholder="123456"
          value={token}
          onChange={handleTokenChange}
          maxLength={6}
          required
          style={{
            width: '100%',
            height: '52px',
            padding: '0 15px',
            border: '1px solid #d8cbb4',
            borderRadius: '8px',
            background: '#efe6d6',
            color: '#1b1916',
            fontSize: '22px',
            fontWeight: '700',
            letterSpacing: '8px',
            textAlign: 'center',
            outline: 'none',
            boxSizing: 'border-box'
          }}
        />

        {error && (
          <div
            style={{
              marginTop: '12px',
              padding: '10px',
              borderRadius: '6px',
              background: '#f8d7da',
              color: '#842029',
              fontSize: '13px'
            }}
          >
            {error}
          </div>
        )}

        <button
          type="submit"
          disabled={
            loading ||
            token.length !== 6
          }
          style={{
            width: '100%',
            height: '48px',
            marginTop: '18px',
            border: 'none',
            borderRadius: '8px',
            background:
              token.length === 6
                ? '#1b1916'
                : '#77716a',
            color: '#ffffff',
            fontWeight: '700',
            cursor:
              token.length === 6
                ? 'pointer'
                : 'not-allowed'
          }}
        >
          {loading
            ? 'Verificando...'
            : 'Verificar correo'}
        </button>
      </form>

      <p
        style={{
          marginTop: '18px',
          marginBottom: '15px',
          color: '#817a70',
          fontSize: '12px'
        }}
      >
        Para esta versión de prueba utiliza el código{' '}
        <strong style={{ color: '#a45a1e' }}>
          123456
        </strong>
      </p>

      <button
        type="button"
        onClick={onVerified}
        style={{
          border: 'none',
          background: 'transparent',
          color: '#a45a1e',
          fontSize: '13px',
          fontWeight: '600',
          cursor: 'pointer',
          display: 'inline-flex',
          alignItems: 'center',
          gap: '6px'
        }}
      >
        <LuArrowLeft />
        Volver al inicio de sesión
      </button>
    </div>
  )
}

export default EmailVerificationNotice