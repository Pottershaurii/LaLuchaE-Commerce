import api from './api'

const login = async (correo, password) => {
  const response = await api.post('/auth/login', {
    correo,
    password
  })

  const token = response.data.token

  if (token) {
    localStorage.setItem('token', token)
  }

  return response.data
}

const logout = () => {
  localStorage.removeItem('token')
}

const getToken = () => {
  return localStorage.getItem('token')
}

const isAuthenticated = () => {
  return !!getToken()
}

const authService = {
  login,
  logout,
  getToken,
  isAuthenticated
}

export default authService