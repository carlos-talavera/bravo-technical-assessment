import { useState } from 'react'
import LoginPage from './pages/LoginPage'
import ApplicationsPage from './pages/ApplicationsPage'

export default function App() {
  const [token, setToken] = useState(() => localStorage.getItem('token'))

  function handleLogin(newToken) {
    localStorage.setItem('token', newToken)
    setToken(newToken)
  }

  function handleLogout() {
    localStorage.removeItem('token')
    setToken(null)
  }

  if (!token) return <LoginPage onLogin={handleLogin} />
  return <ApplicationsPage token={token} onLogout={handleLogout} />
}
