import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Button from '@/components/common/Button'
import GoogleButton, { useGoogleClientId } from '@/components/auth/GoogleButton'
import { useAuth } from '@/contexts/AuthContext'
import type { ApiError } from '@/types'

interface Props {
  mode: 'login' | 'register'
  // Where to go after signing in
  redirectTo: string
}

/**
 * "Continue with Google" on the login and register pages. Shows nothing when the
 * server has no Google client ID.
 *
 * When an account with the same email already exists, the server asks for that
 * account's password once before Google is added to it, and this shows the form.
 */
export default function GoogleSignIn({ mode, redirectTo }: Props) {
  const navigate = useNavigate()
  const { signInWithGoogle, linkGoogle } = useAuth()
  const clientId = useGoogleClientId()

  const [pendingCredential, setPending]   = useState<string | null>(null)
  const [linkMessage, setLinkMessage]     = useState('')
  const [password, setPassword]           = useState('')
  const [error, setError]                 = useState<string | null>(null)
  const [busy, setBusy]                   = useState(false)

  async function handleCredential(credential: string) {
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
      <div className={pendingCredential ? 'hidden' : ''}>
        <GoogleButton
          clientId={clientId}
          text={mode === 'register' ? 'signup_with' : 'continue_with'}
          onCredential={handleCredential}
          onLoadError={() => setError('Google sign-in could not be loaded. You can still use your email and password.')}
        />
      </div>

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
