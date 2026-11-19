import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getGoogleStatus } from '@/api/auth'
import Button from '@/components/common/Button'
import { useAuth } from '@/contexts/AuthContext'
import type { ApiError } from '@/types'

// The parts of Google's sign-in script this page uses
interface GoogleCredentialResponse {
  credential: string
}

interface GoogleAccountsId {
  initialize: (options: { client_id: string; callback: (response: GoogleCredentialResponse) => void }) => void
  renderButton: (parent: HTMLElement, options: Record<string, unknown>) => void
}

declare global {
  interface Window {
    google?: { accounts: { id: GoogleAccountsId } }
  }
}

const GOOGLE_SCRIPT = 'https://accounts.google.com/gsi/client'

// The script is only loaded on the login and register pages, and only once
let scriptPromise: Promise<void> | null = null

function loadGoogleScript(): Promise<void> {
  if (!scriptPromise) {
    scriptPromise = new Promise((resolve, reject) => {
      const script = document.createElement('script')
      script.src = GOOGLE_SCRIPT
      script.async = true
      script.onload = () => resolve()
      script.onerror = () => {
        scriptPromise = null
        reject(new Error('Google sign-in could not be loaded'))
      }
      document.head.appendChild(script)
    })
  }
  return scriptPromise
}

interface Props {
  mode: 'login' | 'register'
  // Where to go after signing in
  redirectTo: string
}

/**
 * "Continue with Google". Shows nothing when the server has no Google client ID.
 *
 * When an account with the same email already exists, the server asks for that
 * account's password once before Google is added to it, and this shows the form.
 */
export default function GoogleSignIn({ mode, redirectTo }: Props) {
  const navigate = useNavigate()
  const { signInWithGoogle, linkGoogle } = useAuth()
  const buttonRef = useRef<HTMLDivElement>(null)

  const [clientId, setClientId]           = useState<string | null>(null)
  const [pendingCredential, setPending]   = useState<string | null>(null)
  const [linkMessage, setLinkMessage]     = useState('')
  const [password, setPassword]           = useState('')
  const [error, setError]                 = useState<string | null>(null)
  const [busy, setBusy]                   = useState(false)

  // The callback below is set up once, so it reads the latest functions from a ref
  const handlerRef = useRef<(credential: string) => void>(() => {})
  handlerRef.current = async (credential: string) => {
    setError(null)
    setBusy(true)
    try {
      await signInWithGoogle(credential)
      navigate(redirectTo, { replace: true })
    } catch (err) {
      const apiError = err as ApiError
      if (apiError?.status === 409) {
        setPending(credential)
        setLinkMessage(apiError.detail)
      } else {
        setError(apiError?.detail ?? 'Google sign-in failed. Please try again.')
      }
    } finally {
      setBusy(false)
    }
  }

  useEffect(() => {
    let ignore = false
    getGoogleStatus()
      .then(status => {
        if (!ignore && status.enabled && status.clientId) setClientId(status.clientId)
      })
      .catch(() => { /* no Google button then; email and password still work */ })
    return () => { ignore = true }
  }, [])

  useEffect(() => {
    if (!clientId) return
    let ignore = false
    loadGoogleScript()
      .then(() => {
        if (ignore || !window.google || !buttonRef.current) return
        window.google.accounts.id.initialize({
          client_id: clientId,
          callback: response => handlerRef.current(response.credential),
        })
        window.google.accounts.id.renderButton(buttonRef.current, {
          theme: 'outline',
          size: 'large',
          text: mode === 'register' ? 'signup_with' : 'continue_with',
          width: Math.min(buttonRef.current.offsetWidth || 320, 400),
        })
      })
      .catch(() => {
        if (!ignore) setError('Google sign-in could not be loaded. You can still use your email and password.')
      })
    return () => { ignore = true }
  }, [clientId, mode])

  async function handleLink(event: React.FormEvent) {
    event.preventDefault()
    if (!pendingCredential || !password) return
    setError(null)
    setBusy(true)
    try {
      await linkGoogle(pendingCredential, password)
      navigate(redirectTo, { replace: true })
    } catch (err) {
      const apiError = err as ApiError
      setError(apiError?.status === 401 ? 'That password is not right.' : apiError?.detail ?? 'Linking failed.')
    } finally {
      setBusy(false)
    }
  }

  if (!clientId) return null

  return (
    <div className="mt-6">
      <div className="flex items-center gap-3 mb-4">
        <div className="h-px flex-1 bg-gray-200" />
        <span className="text-xs text-gray-400">or</span>
        <div className="h-px flex-1 bg-gray-200" />
      </div>

      {/* Kept in the page while the password form shows, so Cancel brings back the
          button Google already drew */}
      <div ref={buttonRef} className={`flex justify-center min-h-[44px] ${pendingCredential ? 'hidden' : ''}`} />

      {pendingCredential && (
        <form onSubmit={handleLink} className="space-y-3" aria-label="Link Google to your account">
          <p className="text-sm text-gray-700">{linkMessage}</p>
          <div>
            <label htmlFor="link-password" className="block text-sm font-medium text-gray-700 mb-1">
              Password of your PA Study Hub account
            </label>
            <input
              id="link-password"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              className="w-full px-4 py-2.5 border border-gray-300 rounded-lg text-sm
                focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>
          <div className="flex gap-2">
            <Button type="submit" loading={busy} className="flex-1">Link and sign in</Button>
            <Button type="button" variant="secondary" onClick={() => { setPending(null); setPassword('') }}>
              Cancel
            </Button>
          </div>
        </form>
      )}

      {error && <p className="mt-3 text-sm text-red-600 text-center" role="alert">{error}</p>}
    </div>
  )
}
