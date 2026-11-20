import { useState, FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import Button from '@/components/common/Button'
import { useAuth } from '@/contexts/AuthContext'
import { useNotification } from '@/contexts/NotificationContext'
import { firstUnmetRequirement } from '@/utils/passwordRules'
import type { ApiError } from '@/types'

// Opened from the reset link in the email: /reset-password?token=...
export default function ResetPasswordPage() {
  const [params]  = useSearchParams()
  const token     = params.get('token') ?? ''
  const navigate  = useNavigate()
  const { resetPassword } = useAuth()
  const { notify } = useNotification()

  const [password, setPassword] = useState('')
  const [confirm, setConfirm]   = useState('')
  const [saving, setSaving]     = useState(false)
  const [error, setError]       = useState<string | null>(null)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    const broken = firstUnmetRequirement(password)
    if (broken) return setError(`New password: ${broken.toLowerCase()}`)
    if (password !== confirm) return setError('The passwords do not match')

    setError(null)
    setSaving(true)
    try {
      await resetPassword(token, password)
      notify('success', 'Password changed. You are signed in.')
      navigate('/dashboard', { replace: true })
    } catch (err) {
      setError((err as ApiError)?.detail ?? 'Could not change the password')
      setSaving(false)
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-primary-50 to-blue-100 flex items-center justify-center p-4">
      <div className="w-full max-w-md bg-white rounded-2xl shadow-lg border border-gray-100 p-8 space-y-5">
        <h1 className="text-2xl font-bold text-gray-900">Choose a new password</h1>

        {!token ? (
          <p className="text-sm text-red-600" role="alert">
            This link is missing its code. Open the link from the email again, or ask for a new one.
          </p>
        ) : (
          <form onSubmit={handleSubmit} noValidate className="space-y-4">
            <div>
              <label htmlFor="new-password" className="block text-sm font-medium text-gray-700 mb-1">New password</label>
              <input id="new-password" type="password" autoComplete="new-password" value={password}
                onChange={e => setPassword(e.target.value)}
                className="w-full px-4 py-2.5 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500" />
              <p className="mt-1 text-xs text-gray-500">
                8 or more characters with an upper case letter, a lower case letter, a number and a special character.
              </p>
            </div>
            <div>
              <label htmlFor="new-password-again" className="block text-sm font-medium text-gray-700 mb-1">New password again</label>
              <input id="new-password-again" type="password" autoComplete="new-password" value={confirm}
                onChange={e => setConfirm(e.target.value)}
                className="w-full px-4 py-2.5 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500" />
            </div>
            {error && <p className="text-sm text-red-600" role="alert">{error}</p>}
            <Button type="submit" loading={saving} className="w-full">Save new password</Button>
          </form>
        )}

        <p className="text-center text-sm text-gray-500">
          <Link to="/forgot-password" className="text-primary-600 font-medium hover:underline">Ask for a new link</Link>
        </p>
      </div>
    </div>
  )
}
