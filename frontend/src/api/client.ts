import axios, { AxiosError, AxiosInstance, InternalAxiosRequestConfig } from 'axios'
import type { ApiError } from '@/types'

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api/v1'
const TOKEN_KEY  = 'pa_access_token'
const REFRESH_KEY = 'pa_refresh_token'

const apiClient: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 30_000,
})

// ---- Request interceptor: attach JWT ----------------------------------------

apiClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// ---- Response interceptor: normalize errors ---------------------------------

apiClient.interceptors.response.use(
  response => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean }

    // A 401 from the login or register call means the details were wrong, not that
    // the session ran out, so the form should show it as an error. These used to go
    // through the redirect below, which reloaded the login page and wiped the message.
    const isAuthRequest = (originalRequest?.url ?? '').startsWith('/auth/')

    // 401 + not already retried → try refresh
    if (error.response?.status === 401 && !originalRequest._retry && !isAuthRequest) {
      const refreshToken = localStorage.getItem(REFRESH_KEY)
      if (refreshToken) {
        originalRequest._retry = true
        try {
          // Backend returns: { accessToken, refreshToken, user }
          const { data } = await axios.post<{ accessToken: string; refreshToken: string }>(`${BASE_URL}/auth/refresh`, { refreshToken })
          localStorage.setItem(TOKEN_KEY, data.accessToken)
          localStorage.setItem(REFRESH_KEY, data.refreshToken)
          originalRequest.headers.Authorization = `Bearer ${data.accessToken}`
          return apiClient(originalRequest)
        } catch {
          // Refresh failed — clear tokens and redirect
          clearTokens()
          window.location.href = '/login'
        }
      } else {
        clearTokens()
        window.location.href = '/login'
      }
    }

    // Normalize error shape
    const apiError: ApiError = {
      status: error.response?.status ?? 0,
      title: 'Request Failed',
      detail: 'An unexpected error occurred.',
      fieldErrors: undefined,
    }

    const data = error.response?.data as Record<string, unknown> | undefined
    if (data) {
      if (typeof data['title']  === 'string') apiError.title  = data['title']
      if (typeof data['detail'] === 'string') apiError.detail = data['detail']
      if (data['fieldErrors'] && typeof data['fieldErrors'] === 'object') {
        apiError.fieldErrors = data['fieldErrors'] as Record<string, string>
      }
    }

    return Promise.reject(apiError)
  }
)

// ---- Token helpers ----------------------------------------------------------

export function setTokens(access: string, refresh: string) {
  localStorage.setItem(TOKEN_KEY, access)
  localStorage.setItem(REFRESH_KEY, refresh)
}

export function clearTokens() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_KEY)
}

export function getAccessToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export default apiClient
