import { createContext, useContext, useState } from 'react'
import {
  loginUser,
  logoutUser,
  isAuthenticated as checkIsAuthenticated
} from '../services/authService'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [isAuthenticated, setIsAuthenticated] = useState(
    checkIsAuthenticated()
  )

  const login = async (correo, password) => {
    const data = await loginUser(correo, password)

    setIsAuthenticated(true)

    return data
  }

  const logout = () => {
    logoutUser()
    setIsAuthenticated(false)
  }

  return (
    <AuthContext.Provider
      value={{
        isAuthenticated,
        login,
        logout
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)

  if (!context) {
    throw new Error(
      'useAuth debe utilizarse dentro de AuthProvider'
    )
  }

  return context
}