import {
  createContext,
  useContext,
  useEffect,
  useState,
  useCallback,
  ReactNode,
} from 'react'
import type { User, LoginRequest, RegisterRequest } from '@/types'
import { login as apiLogin, register as apiRegister, getMe } from '@/api/auth'
import { clearTokens, getAccessToken } from '@/api/client'

interface AuthContextValue {
  user: User | null
  isAuthenticated: boolean
  isLoading: boolean
  login: (req: LoginRequest) => Promise<void>
  register: (req: RegisterRequest) => Promise<void>
  logout: () => void
  updateUser: (updates: Partial<User>) => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  // On mount, try to restore session from stored token
  useEffect(() => {
    const token = getAccessToken()
    if (token) {
      getMe()
        .then(setUser)
        .catch(() => {
          clearTokens()
          setUser(null)
        })
        .finally(() => setIsLoading(false))
    } else {
      setIsLoading(false)
    }
  }, [])

  const login = useCallback(async (req: LoginRequest) => {
    const { user } = await apiLogin(req)
    setUser(user)
  }, [])

  const register = useCallback(async (req: RegisterRequest) => {
    const { user } = await apiRegister(req)
    setUser(user)
  }, [])

  const logout = useCallback(() => {
    clearTokens()
    setUser(null)
    window.location.href = '/login'
  }, [])

  const updateUser = useCallback((updates: Partial<User>) => {
    setUser(prev => (prev ? { ...prev, ...updates } : null))
  }, [])

  return (
    <AuthContext.Provider
      value={{ user, isAuthenticated: !!user, isLoading, login, register, logout, updateUser }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
