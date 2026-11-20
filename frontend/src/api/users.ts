import apiClient, { setTokens } from './client'
import type { User } from '@/types'

export interface ProfileUpdate {
  firstName: string
  lastName: string
  paSchoolName: string
  graduationYear: number | null
}

export async function updateProfile(update: ProfileUpdate): Promise<User> {
  // The server keeps fields that are left out, so an empty year is sent as "clear it"
  const body = update.graduationYear === null ? { ...update, clearGraduationYear: true } : update
  const { data } = await apiClient.put<User>('/users/me', body)
  return data
}

// Changing the password signs out every other device. The server sends new tokens
// for this browser, so it stays signed in.
export async function changePassword(currentPassword: string | null, newPassword: string): Promise<User> {
  const { data } = await apiClient.put<{ accessToken: string; refreshToken: string; user: User }>(
    '/users/me/password',
    { currentPassword, newPassword },
  )
  setTokens(data.accessToken, data.refreshToken)
  return data.user
}

export async function addGoogle(credential: string): Promise<User> {
  const { data } = await apiClient.post<User>('/users/me/google', { credential })
  return data
}

export async function removeGoogle(): Promise<User> {
  const { data } = await apiClient.delete<User>('/users/me/google')
  return data
}

// Needs the password, or for an account without one, its email typed out
export async function deleteAccount(confirmation: { password?: string; confirmEmail?: string }): Promise<void> {
  await apiClient.delete('/users/me', { data: confirmation })
}

// Sends the email confirmation link again (3 an hour)
export async function resendConfirmation(): Promise<void> {
  await apiClient.post('/users/me/confirmation-email')
}
