import apiClient, { getRefreshToken, setTokens } from './client'
import type { LoginRequest, RegisterRequest, User } from '@/types'

interface AuthResponse {
  accessToken: string
  refreshToken: string
  user: User
}

export async function login(req: LoginRequest): Promise<{ user: User }> {
  const { data } = await apiClient.post<AuthResponse>('/auth/login', req)
  setTokens(data.accessToken, data.refreshToken)
  return { user: data.user }
}

export async function register(req: RegisterRequest): Promise<{ user: User }> {
  const { data } = await apiClient.post<AuthResponse>('/auth/register', req)
  setTokens(data.accessToken, data.refreshToken)
  return { user: data.user }
}

// Tells the server to revoke the refresh token, so it can't be used to get new
// access tokens after signing out (for example if it was copied from this browser).
// Short timeout: signing out should not wait a minute for a sleeping server.
export async function logout(): Promise<void> {
  const refreshToken = getRefreshToken()
  if (!refreshToken) return
  await apiClient.post('/auth/logout', { refreshToken }, { timeout: 5_000 })
}

export async function getMe(): Promise<User> {
  const { data } = await apiClient.get<User>('/users/me')
  return data
}
