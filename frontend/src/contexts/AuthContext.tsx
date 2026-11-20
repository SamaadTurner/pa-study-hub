import {
  createContext,
  useContext,
  useEffect,
  useState,
  useCallback,
  ReactNode,
} from 'react'
import type { User, LoginRequest, RegisterRequest } from '@/types'
import {
  login as apiLogin,
  register as apiRegister,
  logout as apiLogout,
  googleSignIn as apiGoogleSignIn,
  googleLink as apiGoogleLink,
  resetPassword as apiResetPassword,
  getMe,
} from '@/api/auth'
import { clearTokens, getAccessToken } from '@/api/client'

interface AuthContextValue {
  user: User | null
  isAuthenticated: boolean
  isLoading: boolean
  login: (req: LoginRequest) => Promise<void>
  register: (req: RegisterRequest) => Promise<void>
  signInWithGoogle: (credential: string) => Promise<void>
  linkGoogle: (credential: string, password: string) => Promise<void>
  resetPassword: (token: string, newPassword: string) => Promise<void>
  logout: () => Promise<void>
  updateUser: (updates: Partial<User>) => void
  // Signs out in this browser only, without calling the server (the account is gone)
  forgetSession: () => void
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

  const signInWithGoogle = useCallback(async (credential: string) => {
    const { user } = await apiGoogleSignIn(credential)
    setUser(user)
  }, [])

  const linkGoogle = useCallback(async (credential: string, password: string) => {
    const { user } = await apiGoogleLink(credential, password)
    setUser(user)
  }, [])

  const resetPassword = useCallback(async (token: string, newPassword: string) => {
    const { user } = await apiResetPassword(token, newPassword)
    setUser(user)
  }, [])

  const logout = useCallback(async () => {
    // Sign out in this browser even if the server can't be reached. The refresh
    // token then stays valid on the server until it expires (7 days).
    try {
      await apiLogout()
    } catch {
      // nothing more to do here
    }
    clearTokens()
    // Only the full page load to /login, which starts the app clean. Clearing the
    // user here as well made the app jump to /login first and reload a moment
    // later, so a click in between was lost.
    window.location.href = '/login'
  }, [])

  const forgetSession = useCallback(() => {
    clearTokens()
    setUser(null)
  }, [])

  const updateUser = useCallback((updates: Partial<User>) => {
    setUser(prev => (prev ? { ...prev, ...updates } : null))
  }, [])

  return (
    <AuthContext.Provider
      value={{
        user, isAuthenticated: !!user, isLoading, login, register, signInWithGoogle, linkGoogle, resetPassword, logout, updateUser, forgetSession,
      }}
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
