import apiClient, { setTokens } from './client'
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

export async function getMe(): Promise<User> {
  const { data } = await apiClient.get<User>('/users/me')
  return data
}
