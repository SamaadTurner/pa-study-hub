import { useState } from 'react'
import { resendConfirmation } from '@/api/users'
import { useAuth } from '@/contexts/AuthContext'
import type { ApiError } from '@/types'

/**
 * Asks a user who has not confirmed their email to do so. Shown above every page
 * behind the login until the email is confirmed; it can be closed for this visit.
 */
export default function ConfirmEmailBanner() {
  const { user } = useAuth()
  const [closed, setClosed]   = useState(false)
  const [sending, setSending] = useState(false)
  const [message, setMessage] = useState<string | null>(null)

  if (!user || user.emailVerified || closed) return null

  async function handleResend() {
    setSending(true)
    try {
      await resendConfirmation()
      setMessage('Sent. Check your inbox and the spam folder.')
    } catch (err) {
      setMessage((err as ApiError)?.detail ?? 'Could not send it right now.')
    } finally {
      setSending(false)
    }
  }

  return (
    <div className="bg-amber-50 border-b border-amber-200" role="region" aria-label="Confirm your email">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-2 flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-amber-900">
        <span>Please confirm your email: we sent a link to {user.email}.</span>
        {message ? (
          <span className="font-medium">{message}</span>
        ) : (
          <button type="button" onClick={handleResend} disabled={sending} className="font-medium underline">
            {sending ? 'Sending...' : 'Send it again'}
          </button>
        )}
        <button type="button" onClick={() => setClosed(true)} className="ml-auto text-amber-700" aria-label="Hide this message">
          ×
        </button>
      </div>
    </div>
  )
}
