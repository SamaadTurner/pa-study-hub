import { useState, FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { requestPasswordReset } from '@/api/auth'
import Button from '@/components/common/Button'
import type { ApiError } from '@/types'

// "Forgot password": asks for the email and sends a reset link. The answer is
// the same whether or not an account uses that email, so nobody can use this page
// to find out who has an account.
export default function ForgotPasswordPage() {
  const [email, setEmail]     = useState('')
  const [sending, setSending] = useState(false)
  const [sent, setSent]       = useState<string | null>(null)
  const [error, setError]     = useState<string | null>(null)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!/^\S+@\S+\.\S+$/.test(email.trim())) {
      setError('Enter the email you signed up with')
      return
    }
    setError(null)
    setSending(true)
    try {
      setSent(await requestPasswordReset(email.trim()))
    } catch (err) {
      setError((err as ApiError)?.detail ?? 'Could not send the link. Please try again.')
    } finally {
      setSending(false)
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-primary-50 to-blue-100 flex items-center justify-center p-4">
      <div className="w-full max-w-md bg-white rounded-2xl shadow-lg border border-gray-100 p-8 space-y-5">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Reset your password</h1>
          <p className="text-sm text-gray-500 mt-1">We'll email you a link to choose a new one.</p>
        </div>

        {sent ? (
          <p className="text-sm text-gray-700 bg-green-50 border border-green-200 rounded-lg p-4" role="status">{sent}</p>
        ) : (
          <form onSubmit={handleSubmit} noValidate className="space-y-4">
            <div>
              <label htmlFor="reset-email" className="block text-sm font-medium text-gray-700 mb-1">Email address</label>
              <input id="reset-email" type="email" autoComplete="email" value={email}
                onChange={e => setEmail(e.target.value)}
                className="w-full px-4 py-2.5 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500" />
            </div>
            {error && <p className="text-sm text-red-600" role="alert">{error}</p>}
            <Button type="submit" loading={sending} className="w-full">Send reset link</Button>
          </form>
        )}

        <p className="text-center text-sm text-gray-500">
          <Link to="/login" className="text-primary-600 font-medium hover:underline">Back to sign in</Link>
        </p>
      </div>
    </div>
  )
}
