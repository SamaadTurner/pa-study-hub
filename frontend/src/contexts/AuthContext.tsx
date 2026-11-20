import {
  createContext,
  useContext,
  useEffect,
  useState,
  useCallback,
  ReactNode,
} from 'react'
import type { ApiError, User, LoginRequest, RegisterRequest } from '@/types'
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

// While the server can't be reached at start-up: try again every 5 seconds, about a minute in all
const RESTORE_ATTEMPTS = 12
const RESTORE_WAIT_MS = 5_000

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  // On mount, try to restore the session from the stored token.
  //
  // Only a real "not signed in" answer (401 or 403, after the client already tried
  // a token refresh) ends the session. Any other failure keeps it and tries again:
  // the free server takes about a minute to wake up, and the first requests time
  // out meanwhile. This used to clear the tokens on any error, which signed
  // students out every time the server had been asleep.
  useEffect(() => {
    if (!getAccessToken()) {
      setIsLoading(false)
      return
    }
    let cancelled = false
    let timer: ReturnType<typeof setTimeout> | undefined

    function restore(attempt: number) {
      getMe()
        .then(me => {
          if (cancelled) return
          setUser(me)
          setIsLoading(false)
        })
        .catch((err: ApiError) => {
          if (cancelled) return
          if (err?.status === 401 || err?.status === 403) {
            clearTokens()
            setUser(null)
            setIsLoading(false)
          } else if (attempt < RESTORE_ATTEMPTS) {
            timer = setTimeout(() => restore(attempt + 1), RESTORE_WAIT_MS)
          } else {
            // Still unreachable after about a minute: stop waiting, keep the tokens
            setIsLoading(false)
          }
        })
    }

    restore(1)
    return () => {
      cancelled = true
      if (timer) clearTimeout(timer)
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
