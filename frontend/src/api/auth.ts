import apiClient, { setTokens } from './client'
import type { AuthTokens, LoginRequest, RegisterRequest, User } from '@/types'

export async function login(req: LoginRequest): Promise<{ user: User; tokens: AuthTokens }> {
  const { data } = await apiClient.post<{ user: User; tokens: AuthTokens }>('/auth/login', req)
  setTokens(data.tokens.accessToken, data.tokens.refreshToken)
  return data
}

export async function register(req: RegisterRequest): Promise<{ user: User; tokens: AuthTokens }> {
  const { data } = await apiClient.post<{ user: User; tokens: AuthTokens }>('/auth/register', req)
  setTokens(data.tokens.accessToken, data.tokens.refreshToken)
  return data
}

export async function getMe(): Promise<User> {
  const { data } = await apiClient.get<User>('/users/me')
  return data
}
