import { useEffect, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { confirmEmail, getMe } from '@/api/auth'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import { useAuth } from '@/contexts/AuthContext'
import type { ApiError } from '@/types'

// Opened from the link in the sign-up email: /confirm-email?token=...
export default function ConfirmEmailPage() {
  const [params] = useSearchParams()
  const token    = params.get('token') ?? ''
  const { user, updateUser } = useAuth()

  const [state, setState] = useState<'working' | 'done' | 'failed'>('working')
  const [error, setError] = useState<string | null>(null)

  // A link works once. React runs effects twice in development, so this makes
  // sure the link is only sent to the server one time.
  const sentRef = useRef(false)

  useEffect(() => {
    if (sentRef.current) return
    sentRef.current = true
    if (!token) {
      setState('failed')
      setError('This link is missing its code. Open the link from the email again.')
      return
    }
    confirmEmail(token)
      .then(async () => {
        setState('done')
        // Signed in in this browser: show the confirmed state right away
        if (localStorage.getItem('pa_access_token')) {
          try {
            updateUser(await getMe())
          } catch {
            // not signed in after all; nothing to update
          }
        }
      })
      .catch((err: ApiError) => {
        setState('failed')
        setError(err?.detail ?? 'Could not confirm the email')
      })
  }, [token, updateUser])

  return (
    <div className="min-h-screen bg-gradient-to-br from-primary-50 to-blue-100 flex items-center justify-center p-4">
      <div className="w-full max-w-md bg-white rounded-2xl shadow-lg border border-gray-100 p-8 space-y-5 text-center">
        {state === 'working' && <LoadingSpinner message="Confirming your email..." />}
        {state === 'done' && (
          <>
            <h1 className="text-2xl font-bold text-gray-900">Email confirmed</h1>
            <p className="text-sm text-gray-600">Thanks. Your email is confirmed.</p>
            <Link to={user ? '/dashboard' : '/login'} className="inline-block text-primary-600 font-medium hover:underline">
              {user ? 'Go to the dashboard' : 'Sign in'}
            </Link>
          </>
        )}
        {state === 'failed' && (
          <>
            <h1 className="text-2xl font-bold text-gray-900">That didn't work</h1>
            <p className="text-sm text-red-600" role="alert">{error}</p>
            <Link to="/dashboard" className="inline-block text-primary-600 font-medium hover:underline">
              Go to the app
            </Link>
          </>
        )}
      </div>
    </div>
  )
}
