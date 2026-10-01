const API_URL = 'http://localhost:8080/api/auth'

/* ========================================
   FUNCIÓN AUXILIAR PARA LEER RESPUESTAS
======================================== */
const getResponseData = async (response) => {
  const contentType = response.headers.get('content-type')

  if (contentType && contentType.includes('application/json')) {
    return await response.json()
  }

  const text = await response.text()

  return {
    success: response.ok,
    message: text || 'Ocurrió un error en el servidor.'
  }
}

/* ========================================
   REGISTRO DE USUARIO
======================================== */
export const registerClient = async (userData) => {
  try {
    const response = await fetch(`${API_URL}/register`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        nombre: userData.nombre,
        email: userData.email,
        password: userData.password
      })
    })

    const data = await getResponseData(response)

    if (!response.ok) {
      throw new Error(
        data.message || 'No se pudo completar el registro.'
      )
    }

    return data
  } catch (error) {
    if (error instanceof TypeError) {
      throw new Error('No se pudo conectar con el servidor.')
    }

    throw error
  }
}

/* ========================================
   VERIFICACIÓN DE CORREO
======================================== */
export const verifyEmailToken = async (email, code) => {
  try {
    const response = await fetch(`${API_URL}/verify`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        email,
        code
      })
    })

    const data = await getResponseData(response)

    if (!response.ok) {
      throw new Error(
        data.message || 'El código de verificación es incorrecto.'
      )
    }

    return data
  } catch (error) {
    if (error instanceof TypeError) {
      throw new Error('No se pudo conectar con el servidor.')
    }

    throw error
  }
}

/* ========================================
   LOGIN
   Temporal hasta conectar login al backend
======================================== */
export const loginUser = async (correo, password) => {
  if (!correo || !password) {
    throw new Error('Debes ingresar correo y contraseña.')
  }

  const authenticatedUser = {
    email: correo,
    nombre: 'Usuario La Lucha'
  }

  localStorage.setItem(
    'lalucha_user',
    JSON.stringify(authenticatedUser)
  )

  localStorage.setItem(
    'lalucha_authenticated',
    'true'
  )

  return {
    success: true,
    user: authenticatedUser,
    message: 'Inicio de sesión correcto.'
  }
}

/* ========================================
   CERRAR SESIÓN
======================================== */
export const logoutUser = () => {
  localStorage.removeItem('lalucha_user')
  localStorage.removeItem('lalucha_authenticated')
}

/* ========================================
   COMPROBAR SESIÓN
======================================== */
export const isAuthenticated = () => {
  return (
    localStorage.getItem('lalucha_authenticated') === 'true'
  )
}

/* ========================================
   OBTENER USUARIO ACTUAL
======================================== */
export const getCurrentUser = () => {
  const user = localStorage.getItem('lalucha_user')

  if (!user) {
    return null
  }

  try {
    return JSON.parse(user)
  } catch {
    return null
  }
}