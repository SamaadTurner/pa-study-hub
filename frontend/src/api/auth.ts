import apiClient, { getLegacyRefreshToken, setTokens } from './client'
import type { LoginRequest, RegisterRequest, User } from '@/types'

interface AuthResponse {
  accessToken: string
  // The refresh token comes as an HttpOnly cookie, not in the JSON
  user: User
}

export async function login(req: LoginRequest): Promise<{ user: User }> {
  const { data } = await apiClient.post<AuthResponse>('/auth/login', req)
  setTokens(data.accessToken)
  return { user: data.user }
}

export async function register(req: RegisterRequest): Promise<{ user: User }> {
  const { data } = await apiClient.post<AuthResponse>('/auth/register', req)
  setTokens(data.accessToken)
  return { user: data.user }
}

// ---- Continue with Google ------------------------------------------------------

export interface GoogleStatus {
  enabled: boolean
  clientId: string | null
}

export async function getGoogleStatus(): Promise<GoogleStatus> {
  const { data } = await apiClient.get<GoogleStatus>('/auth/google/status')
  return data
}

// credential is the ID token Google's button hands to the page
export async function googleSignIn(credential: string): Promise<{ user: User }> {
  const { data } = await apiClient.post<AuthResponse>('/auth/google', { credential })
  setTokens(data.accessToken)
  return { user: data.user }
}

// Adds Google to an existing account, proven with that account's password
export async function googleLink(credential: string, password: string): Promise<{ user: User }> {
  const { data } = await apiClient.post<AuthResponse>('/auth/google/link', { credential, password })
  setTokens(data.accessToken)
  return { user: data.user }
}

// ---- Links sent by email ---------------------------------------------------------

// "Forgot password". Returns the sentence to show, the same whether or not the email has an account.
export async function requestPasswordReset(email: string): Promise<string> {
  const { data } = await apiClient.post<{ message: string }>('/auth/password-reset/request', { email })
  return data.message
}

// Sets the new password from a reset link and signs in
export async function resetPassword(token: string, newPassword: string): Promise<{ user: User }> {
  const { data } = await apiClient.post<AuthResponse>('/auth/password-reset/confirm', { token, newPassword })
  setTokens(data.accessToken)
  return { user: data.user }
}

export async function confirmEmail(token: string): Promise<void> {
  await apiClient.post('/auth/confirm-email', { token })
}

// Tells the server to revoke the refresh token, so it can't be used to get new
// access tokens after signing out (for example if it was copied from this browser).
// Short timeout: signing out should not wait a minute for a sleeping server.
export async function logout(): Promise<void> {
  // The cookie goes along by itself and the server deletes it; a token left over
  // from an older version is sent in the body
  const legacy = getLegacyRefreshToken()
  await apiClient.post('/auth/logout', legacy ? { refreshToken: legacy } : {}, { timeout: 5_000 })
}

export async function getMe(): Promise<User> {
  const { data } = await apiClient.get<User>('/users/me')
  return data
}
