import { createContext, useContext, useEffect, useState, type PropsWithChildren } from 'react'
import type { LoginRequest, RegisterRequest, User } from './types'
import { useApi } from '@/app/providers'

interface AuthContextValue {
  user: User | null
  isInitializing: boolean
  isAuthenticated: boolean
  login: (request: LoginRequest) => Promise<void>
  register: (request: RegisterRequest) => Promise<void>
  logout: () => Promise<void>
}
const AuthContext = createContext<AuthContextValue | null>(null)

export function useAuth() {
  const value = useContext(AuthContext)
  if (!value) throw new Error('useAuth must be used within AuthProvider')
  return value
}

export function AuthProvider({ children }: PropsWithChildren) {
  const { auth } = useApi()
  const [user, setUser] = useState<User | null>(null)
  const [isInitializing, setIsInitializing] = useState(true)
  useEffect(() => {
    let active = true
    auth.getCurrentUser().then((current) => { if (active) setUser(current) }).catch(() => { if (active) setUser(null) }).finally(() => { if (active) setIsInitializing(false) })
    return () => { active = false }
  }, [auth])
  const login = async (request: LoginRequest) => { const response = await auth.login(request); setUser(response.user) }
  const register = async (request: RegisterRequest) => { const response = await auth.register(request); setUser(response.user) }
  const logout = async () => { await auth.logout(); setUser(null) }
  return <AuthContext.Provider value={{ user, isInitializing, isAuthenticated: Boolean(user), login, register, logout }}>{children}</AuthContext.Provider>
}
