import { useState, FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { addGoogle, changePassword, deleteAccount, removeGoogle, updateProfile } from '@/api/users'
import { deviceTimeZone } from '@/api/client'
import Button from '@/components/common/Button'
import GoogleButton, { useGoogleClientId } from '@/components/auth/GoogleButton'
import { useAuth } from '@/contexts/AuthContext'
import { useNotification } from '@/contexts/NotificationContext'
import { firstUnmetRequirement } from '@/utils/passwordRules'
import type { ApiError } from '@/types'

const inputClass = 'w-full px-4 py-2.5 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500'
const sectionClass = 'bg-white border border-gray-200 rounded-xl p-6 space-y-4'

export default function ProfilePage() {
  const { user, updateUser, forgetSession } = useAuth()
  const { notify } = useNotification()
  const navigate = useNavigate()
  const googleClientId = useGoogleClientId()

  // ---- Profile ---------------------------------------------------------------
  const [firstName, setFirstName] = useState(user?.firstName ?? '')
  const [lastName, setLastName]   = useState(user?.lastName ?? '')
  const [school, setSchool]       = useState(user?.paSchoolName ?? '')
  const [gradYear, setGradYear]   = useState(user?.graduationYear ? String(user.graduationYear) : '')
  const [savingProfile, setSavingProfile] = useState(false)
  const [profileError, setProfileError]   = useState<string | null>(null)

  // ---- Password ----------------------------------------------------------------
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword]         = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [savingPassword, setSavingPassword]   = useState(false)
  const [passwordError, setPasswordError]     = useState<string | null>(null)

  // ---- Google and closing the account ---------------------------------------------
  const [googleBusy, setGoogleBusy]   = useState(false)
  const [googleError, setGoogleError] = useState<string | null>(null)
  const [confirmation, setConfirmation] = useState('')
  const [deleting, setDeleting]         = useState(false)
  const [deleteError, setDeleteError]   = useState<string | null>(null)

  if (!user) return null

  async function handleProfile(event: FormEvent) {
    event.preventDefault()
    const year = gradYear.trim() === '' ? null : Number(gradYear)
    if (!firstName.trim()) return setProfileError('First name is required')
    if (year !== null && (!Number.isInteger(year) || year < 2020 || year > 2040)) {
      return setProfileError('Graduation year must be between 2020 and 2040')
    }
    setProfileError(null)
    setSavingProfile(true)
    try {
      const saved = await updateProfile({
        firstName: firstName.trim(), lastName: lastName.trim(), paSchoolName: school.trim(), graduationYear: year,
      })
      updateUser(saved)
      notify('success', 'Profile saved')
    } catch (err) {
      setProfileError((err as ApiError)?.detail ?? 'Could not save the profile')
    } finally {
      setSavingProfile(false)
    }
  }

  async function handlePassword(event: FormEvent) {
    event.preventDefault()
    if (user!.hasPassword && !currentPassword) return setPasswordError('Enter your current password')
    const broken = firstUnmetRequirement(newPassword)
    if (broken) return setPasswordError(`New password: ${broken.toLowerCase()}`)
    if (newPassword !== confirmPassword) return setPasswordError('The new passwords do not match')

    setPasswordError(null)
    setSavingPassword(true)
    try {
      const saved = await changePassword(user!.hasPassword ? currentPassword : null, newPassword)
      updateUser(saved)
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
      notify('success', user!.hasPassword
        ? 'Password changed. Other devices are signed out.'
        : 'Password set. You can now also sign in with your email.')
    } catch (err) {
      setPasswordError((err as ApiError)?.detail ?? 'Could not change the password')
    } finally {
      setSavingPassword(false)
    }
  }

  async function handleAddGoogle(credential: string) {
    setGoogleError(null)
    setGoogleBusy(true)
    try {
      updateUser(await addGoogle(credential))
      notify('success', 'Google sign-in added')
    } catch (err) {
      setGoogleError((err as ApiError)?.detail ?? 'Could not add Google')
    } finally {
      setGoogleBusy(false)
    }
  }

  async function handleRemoveGoogle() {
    setGoogleError(null)
    setGoogleBusy(true)
    try {
      updateUser(await removeGoogle())
      notify('success', 'Google sign-in removed')
    } catch (err) {
      setGoogleError((err as ApiError)?.detail ?? 'Could not remove Google')
    } finally {
      setGoogleBusy(false)
    }
  }

  async function handleDelete(event: FormEvent) {
    event.preventDefault()
    if (!confirmation) return
    const sure = window.confirm('Delete your account and all your study data? This cannot be undone.')
    if (!sure) return
    setDeleteError(null)
    setDeleting(true)
    try {
      await deleteAccount(user!.hasPassword ? { password: confirmation } : { confirmEmail: confirmation })
      forgetSession()
      notify('success', 'Your account and data were deleted')
      navigate('/login', { replace: true })
    } catch (err) {
      setDeleteError((err as ApiError)?.detail ?? 'Could not delete the account')
      setDeleting(false)
    }
  }

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Profile</h1>
        <p className="text-sm text-gray-500 mt-1">{user.email}</p>
      </div>

      <form onSubmit={handleProfile} className={sectionClass} aria-label="Profile">
        <h2 className="text-sm font-semibold text-gray-900">Your details</h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label htmlFor="firstName" className="block text-sm font-medium text-gray-700 mb-1">First name</label>
            <input id="firstName" value={firstName} maxLength={50} onChange={e => setFirstName(e.target.value)} className={inputClass} />
          </div>
          <div>
            <label htmlFor="lastName" className="block text-sm font-medium text-gray-700 mb-1">Last name</label>
            <input id="lastName" value={lastName} maxLength={50} onChange={e => setLastName(e.target.value)} className={inputClass} />
          </div>
          <div>
            <label htmlFor="school" className="block text-sm font-medium text-gray-700 mb-1">
              PA school <span className="text-gray-500 font-normal">(optional)</span>
            </label>
            <input id="school" value={school} maxLength={200} onChange={e => setSchool(e.target.value)} className={inputClass} />
          </div>
          <div>
            <label htmlFor="gradYear" className="block text-sm font-medium text-gray-700 mb-1">
              Graduation year <span className="text-gray-500 font-normal">(optional)</span>
            </label>
            <input id="gradYear" inputMode="numeric" value={gradYear} onChange={e => setGradYear(e.target.value)} className={inputClass} />
          </div>
        </div>
        <p className="text-sm text-gray-600">
          Time zone: <span className="font-medium text-gray-800">{deviceTimeZone() ?? user.timeZone ?? 'UTC'}</span>.
          Taken from this device; streaks, the daily goal and due dates follow it.
        </p>
        {profileError && <p className="text-sm text-red-600" role="alert">{profileError}</p>}
        <Button type="submit" loading={savingProfile}>Save details</Button>
      </form>

      <form onSubmit={handlePassword} className={sectionClass} aria-label="Password">
        <h2 className="text-sm font-semibold text-gray-900">{user.hasPassword ? 'Change password' : 'Set a password'}</h2>
        {!user.hasPassword && (
          <p className="text-sm text-gray-600">
            You sign in with Google. A password lets you sign in with your email too.
          </p>
        )}
        {user.hasPassword && (
          <div>
            <label htmlFor="currentPassword" className="block text-sm font-medium text-gray-700 mb-1">Current password</label>
            <input id="currentPassword" type="password" autoComplete="current-password" value={currentPassword}
              onChange={e => setCurrentPassword(e.target.value)} className={inputClass} />
          </div>
        )}
        <div>
          <label htmlFor="newPassword" className="block text-sm font-medium text-gray-700 mb-1">New password</label>
          <input id="newPassword" type="password" autoComplete="new-password" value={newPassword}
            onChange={e => setNewPassword(e.target.value)} className={inputClass} />
          <p className="mt-1 text-xs text-gray-500">
            8 or more characters with an upper case letter, a lower case letter, a number and a special character.
          </p>
        </div>
        <div>
          <label htmlFor="confirmPassword" className="block text-sm font-medium text-gray-700 mb-1">New password again</label>
          <input id="confirmPassword" type="password" autoComplete="new-password" value={confirmPassword}
            onChange={e => setConfirmPassword(e.target.value)} className={inputClass} />
        </div>
        {passwordError && <p className="text-sm text-red-600" role="alert">{passwordError}</p>}
        <Button type="submit" loading={savingPassword}>{user.hasPassword ? 'Change password' : 'Set password'}</Button>
      </form>

      {(googleClientId || user.googleLinked) && (
        <section className={sectionClass} aria-label="Google sign-in">
          <h2 className="text-sm font-semibold text-gray-900">Google sign-in</h2>
          {user.googleLinked ? (
            <>
              <p className="text-sm text-gray-600">Google sign-in is on for this account.</p>
              {user.hasPassword ? (
                <Button variant="secondary" loading={googleBusy} onClick={handleRemoveGoogle}>Remove Google sign-in</Button>
              ) : (
                <p className="text-xs text-gray-500">Set a password first if you want to remove Google, so you can still sign in.</p>
              )}
            </>
          ) : (
            <>
              <p className="text-sm text-gray-600">Add Google to sign in with one click. Your email and password keep working.</p>
              {googleClientId && (
                <GoogleButton
                  clientId={googleClientId}
                  text="continue_with"
                  onCredential={handleAddGoogle}
                  onLoadError={() => setGoogleError('Google sign-in could not be loaded.')}
                />
              )}
            </>
          )}
          {googleError && <p className="text-sm text-red-600" role="alert">{googleError}</p>}
        </section>
      )}

      <form onSubmit={handleDelete} className="bg-white border border-red-200 rounded-xl p-6 space-y-4" aria-label="Close account">
        <h2 className="text-sm font-semibold text-red-700">Close account</h2>
        <p className="text-sm text-gray-600">
          Deletes your account and all your study data: decks, cards, reviews, exams and the questions you wrote.
          Copies other people made of your public decks stay theirs. This cannot be undone.
        </p>
        <div>
          <label htmlFor="deleteConfirmation" className="block text-sm font-medium text-gray-700 mb-1">
            {user.hasPassword ? 'Your password' : 'Type your email to confirm'}
          </label>
          <input id="deleteConfirmation" type={user.hasPassword ? 'password' : 'email'}
            autoComplete={user.hasPassword ? 'current-password' : 'off'}
            value={confirmation} onChange={e => setConfirmation(e.target.value)} className={inputClass} />
        </div>
        {deleteError && <p className="text-sm text-red-600" role="alert">{deleteError}</p>}
        <Button type="submit" variant="danger" loading={deleting} disabled={!confirmation}>
          Delete my account
        </Button>
      </form>
    </div>
  )
}
