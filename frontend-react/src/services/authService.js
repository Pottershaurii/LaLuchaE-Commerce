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
   Llama a POST /api/auth/login y guarda el token JWT
======================================== */
export const loginUser = async (correo, password) => {
  if (!correo || !password) {
    throw new Error('Debes ingresar correo y contraseña.')
  }

  try {
    const response = await fetch(`${API_URL}/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        email: correo,
        password
      })
    })

    const data = await getResponseData(response)

    if (!response.ok || !data.success) {
      throw new Error(
        data.message || 'No se pudo iniciar sesión.'
      )
    }

    const authenticatedUser = {
      email: data.email,
      nombre: data.nombre,
      rol: data.rol
    }

    // api.js lee el token desde 'token' para enviarlo como Bearer
    localStorage.setItem('token', data.token)

    localStorage.setItem(
      'lalucha_user',
      JSON.stringify(authenticatedUser)
    )

    return {
      ...data,
      user: authenticatedUser
    }
  } catch (error) {
    if (error instanceof TypeError) {
      throw new Error('No se pudo conectar con el servidor.')
    }

    throw error
  }
}

/* ========================================
   CERRAR SESIÓN
======================================== */
export const logoutUser = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('lalucha_user')
  localStorage.removeItem('lalucha_authenticated')
}

/* ========================================
   COMPROBAR SESIÓN
   Hay sesión si existe un token JWT que no haya vencido
======================================== */
export const isAuthenticated = () => {
  const token = localStorage.getItem('token')

  if (!token) {
    return false
  }

  try {
    const payload = JSON.parse(
      atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/'))
    )

    if (payload.exp && payload.exp * 1000 < Date.now()) {
      logoutUser()
      return false
    }

    return true
  } catch {
    logoutUser()
    return false
  }
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
