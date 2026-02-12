import { useState, FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '@/contexts/AuthContext'
import { useNotification } from '@/contexts/NotificationContext'
import type { ApiError } from '@/types'
import Button from '@/components/common/Button'

interface FormState {
  email: string
  password: string
  confirmPassword: string
  firstName: string
  lastName: string
  paSchoolName: string
  graduationYear: string
}

interface FieldErrors {
  email?: string
  password?: string
  confirmPassword?: string
  firstName?: string
  lastName?: string
}

interface PasswordRequirement {
  label: string
  test: (pw: string) => boolean
}

const PASSWORD_REQUIREMENTS: PasswordRequirement[] = [
  { label: 'At least 8 characters',             test: pw => pw.length >= 8 },
  { label: 'Contains uppercase letter',          test: pw => /[A-Z]/.test(pw) },
  { label: 'Contains lowercase letter',          test: pw => /[a-z]/.test(pw) },
  { label: 'Contains a number',                  test: pw => /\d/.test(pw) },
  { label: 'Contains a special character',       test: pw => /[!@#$%^&*(),.?":{}|<>]/.test(pw) },
]

export default function RegisterPage() {
  const navigate   = useNavigate()
  const { register } = useAuth()
  const { notify }   = useNotification()

  const [form, setForm]       = useState<FormState>({
    email: '', password: '', confirmPassword: '',
    firstName: '', lastName: '', paSchoolName: '', graduationYear: '',
  })
  const [errors, setErrors]   = useState<FieldErrors>({})
  const [loading, setLoading] = useState(false)

  function validate(): boolean {
    const next: FieldErrors = {}
    if (!form.firstName.trim())                next.firstName = 'First name is required'
    if (!form.lastName.trim())                 next.lastName  = 'Last name is required'
    if (!form.email)                           next.email     = 'Email is required'
    else if (!/\S+@\S+\.\S+/.test(form.email))next.email     = 'Enter a valid email address'
    if (!form.password)                        next.password  = 'Password is required'
    else {
      const failed = PASSWORD_REQUIREMENTS.filter(r => !r.test(form.password))
      if (failed.length > 0) next.password = failed[0].label
    }
    if (form.password !== form.confirmPassword) {
      next.confirmPassword = 'Passwords do not match'
    }
    setErrors(next)
    return Object.keys(next).length === 0
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (!validate()) return

    setLoading(true)
    try {
      await register({
        email:        form.email,
        password:     form.password,
        firstName:    form.firstName.trim(),
        lastName:     form.lastName.trim(),
      })
      navigate('/dashboard', { replace: true })
    } catch (err) {
      const apiError = err as ApiError
      if (apiError?.fieldErrors) {
        setErrors(apiError.fieldErrors as FieldErrors)
      } else {
        notify('error', apiError?.detail ?? 'Registration failed. Please try again.')
      }
    } finally {
      setLoading(false)
    }
  }

  const metRequirements = PASSWORD_REQUIREMENTS.filter(r => r.test(form.password))

  return (
    <div className="min-h-screen bg-gradient-to-br from-primary-50 to-blue-100 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        {/* Logo */}
        <div className="text-center mb-8">
          <div className="text-5xl mb-3">🩺</div>
          <h1 className="text-2xl font-bold text-gray-900">PA Study Hub</h1>
          <p className="text-gray-500 text-sm mt-1">Create your study account</p>
        </div>

        <div className="bg-white rounded-2xl shadow-lg border border-gray-100 p-8">
          <form onSubmit={handleSubmit} noValidate className="space-y-4">
            {/* Name row */}
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label htmlFor="firstName" className="block text-sm font-medium text-gray-700 mb-1">
                  First name
                </label>
                <input
                  id="firstName"
                  type="text"
                  autoComplete="given-name"
                  value={form.firstName}
                  onChange={e => setForm(f => ({ ...f, firstName: e.target.value }))}
                  className={`w-full px-3 py-2.5 border rounded-lg text-sm focus:outline-none
                    focus:ring-2 focus:ring-primary-500 focus:border-transparent
                    ${errors.firstName ? 'border-red-400 bg-red-50' : 'border-gray-300'}`}
                  placeholder="Jocelyn"
                />
                {errors.firstName && (
                  <p className="mt-1 text-xs text-red-600">{errors.firstName}</p>
                )}
              </div>
              <div>
                <label htmlFor="lastName" className="block text-sm font-medium text-gray-700 mb-1">
                  Last name
                </label>
                <input
                  id="lastName"
                  type="text"
                  autoComplete="family-name"
                  value={form.lastName}
                  onChange={e => setForm(f => ({ ...f, lastName: e.target.value }))}
                  className={`w-full px-3 py-2.5 border rounded-lg text-sm focus:outline-none
                    focus:ring-2 focus:ring-primary-500 focus:border-transparent
                    ${errors.lastName ? 'border-red-400 bg-red-50' : 'border-gray-300'}`}
                  placeholder="Smith"
                />
                {errors.lastName && (
                  <p className="mt-1 text-xs text-red-600">{errors.lastName}</p>
                )}
              </div>
            </div>

            {/* Email */}
            <div>
              <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-1">
                Email address
              </label>
              <input
                id="email"
                type="email"
                autoComplete="email"
                value={form.email}
                onChange={e => setForm(f => ({ ...f, email: e.target.value }))}
                className={`w-full px-4 py-2.5 border rounded-lg text-sm focus:outline-none
                  focus:ring-2 focus:ring-primary-500 focus:border-transparent
                  ${errors.email ? 'border-red-400 bg-red-50' : 'border-gray-300'}`}
                placeholder="jocelyn@example.com"
              />
              {errors.email && (
                <p className="mt-1 text-xs text-red-600">{errors.email}</p>
              )}
            </div>

            {/* Password */}
            <div>
              <label htmlFor="password" className="block text-sm font-medium text-gray-700 mb-1">
                Password
              </label>
              <input
                id="password"
                type="password"
                autoComplete="new-password"
                value={form.password}
                onChange={e => setForm(f => ({ ...f, password: e.target.value }))}
                className={`w-full px-4 py-2.5 border rounded-lg text-sm focus:outline-none
                  focus:ring-2 focus:ring-primary-500 focus:border-transparent
                  ${errors.password ? 'border-red-400 bg-red-50' : 'border-gray-300'}`}
                placeholder="••••••••"
              />
              {/* Password strength indicators */}
              {form.password && (
                <div className="mt-2 grid grid-cols-2 gap-1">
                  {PASSWORD_REQUIREMENTS.map(req => {
                    const met = req.test(form.password)
                    return (
                      <div key={req.label} className="flex items-center gap-1">
                        <span className={`text-xs ${met ? 'text-green-600' : 'text-gray-400'}`}>
                          {met ? '✓' : '○'}
                        </span>
                        <span className={`text-xs ${met ? 'text-green-700' : 'text-gray-400'}`}>
                          {req.label}
                        </span>
                      </div>
                    )
                  })}
                </div>
              )}
              {errors.password && !form.password && (
                <p className="mt-1 text-xs text-red-600">{errors.password}</p>
              )}
            </div>

            {/* Confirm Password */}
            <div>
              <label htmlFor="confirmPassword" className="block text-sm font-medium text-gray-700 mb-1">
                Confirm password
              </label>
              <input
                id="confirmPassword"
                type="password"
                autoComplete="new-password"
                value={form.confirmPassword}
                onChange={e => setForm(f => ({ ...f, confirmPassword: e.target.value }))}
                className={`w-full px-4 py-2.5 border rounded-lg text-sm focus:outline-none
                  focus:ring-2 focus:ring-primary-500 focus:border-transparent
                  ${errors.confirmPassword ? 'border-red-400 bg-red-50' : 'border-gray-300'}`}
                placeholder="••••••••"
              />
              {errors.confirmPassword && (
                <p className="mt-1 text-xs text-red-600">{errors.confirmPassword}</p>
              )}
            </div>

            {/* Optional PA school info */}
            <div>
              <label htmlFor="paSchoolName" className="block text-sm font-medium text-gray-700 mb-1">
                PA School <span className="text-gray-400 font-normal">(optional)</span>
              </label>
              <input
                id="paSchoolName"
                type="text"
                value={form.paSchoolName}
                onChange={e => setForm(f => ({ ...f, paSchoolName: e.target.value }))}
                className="w-full px-4 py-2.5 border border-gray-300 rounded-lg text-sm
                  focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                placeholder="University of Washington"
              />
            </div>

            <Button
              type="submit"
              loading={loading}
              className="w-full"
              disabled={form.password.length > 0 && metRequirements.length < PASSWORD_REQUIREMENTS.length}
            >
              Create account
            </Button>
          </form>

          <p className="text-center text-sm text-gray-500 mt-6">
            Already have an account?{' '}
            <Link to="/login" className="text-primary-600 font-medium hover:underline">
              Sign in
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}
