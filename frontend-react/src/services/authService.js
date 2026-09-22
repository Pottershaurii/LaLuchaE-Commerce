const mockUsersDatabase = []

/* ========================================
   REGISTRO DE USUARIO
======================================== */

export const registerClient = async (userData) => {
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      const existingUser = mockUsersDatabase.find(
        (user) => user.email === userData.email
      )

      if (existingUser) {
        return reject(
          new Error(
            'El correo electrónico ya se encuentra registrado.'
          )
        )
      }

      const newUser = {
        ...userData,
        id: Date.now(),
        verified: false,
        token: '123456'
      }

      mockUsersDatabase.push(newUser)

      console.log('--- [MOCK EMAIL SENT] ---')
      console.log(`Para: ${userData.email}`)
      console.log('Código de verificación: 123456')

      resolve({
        success: true,
        message:
          'Usuario registrado correctamente. Revisa tu correo.',
        email: userData.email
      })
    }, 1500)
  })
}

/* ========================================
   VERIFICACIÓN DE CORREO
======================================== */

export const verifyEmailToken = async (token) => {
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      if (token === '123456') {
        resolve({
          success: true,
          message: 'Correo verificado exitosamente.'
        })
      } else {
        reject(
          new Error(
            'El código ingresado es incorrecto. Usa: 123456'
          )
        )
      }
    }, 1000)
  })
}

/* ========================================
   LOGIN
======================================== */

export const loginUser = async (correo, password) => {
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      if (!correo || !password) {
        reject(
          new Error('Debes ingresar correo y contraseña.')
        )
        return
      }

      const user = mockUsersDatabase.find(
        (usuario) =>
          usuario.email === correo &&
          usuario.password === password
      )

      /*
       * Mientras no exista conexión real con backend,
       * permitimos el acceso simulado.
       */

      const authenticatedUser =
        user || {
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

      resolve({
        success: true,
        user: authenticatedUser,
        message: 'Inicio de sesión correcto.'
      })
    }, 1000)
  })
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
    localStorage.getItem('lalucha_authenticated') ===
    'true'
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