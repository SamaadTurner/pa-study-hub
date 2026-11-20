import axios, { AxiosError, AxiosInstance, InternalAxiosRequestConfig } from 'axios'
import type { ApiError } from '@/types'
import { isServerUnreachable, SERVER_UNREACHABLE_MESSAGE, slowRequests } from '@/utils/serverStatus'

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api/v1'
const TOKEN_KEY  = 'pa_access_token'
// The refresh token now lives in an HttpOnly cookie the page can't read. Older
// versions kept it here; one left over is handed to the server once and removed.
const LEGACY_REFRESH_KEY = 'pa_refresh_token'

const apiClient: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 30_000,
})

// ---- Request interceptor: attach JWT ----------------------------------------

// The device's time zone, for example America/Los_Angeles. The server runs on UTC
// and uses this for "today": streaks, the daily goal and due dates.
export function deviceTimeZone(): string | null {
  try {
    return Intl.DateTimeFormat().resolvedOptions().timeZone || null
  } catch {
    return null
  }
}

// Remembers which waiting request is which, so the banner knows when all are done
type TrackedConfig = InternalAxiosRequestConfig & { _slowId?: number }

function finishTracking(config: TrackedConfig | undefined) {
  if (config?._slowId !== undefined) slowRequests.end(config._slowId)
}

apiClient.interceptors.request.use((config: TrackedConfig) => {
  config._slowId = slowRequests.start()
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  const zone = deviceTimeZone()
  if (zone) {
    config.headers['X-Time-Zone'] = zone
  }
  return config
})

// ---- Token refresh, once for many requests -----------------------------------

// When the access token runs out, a page that loads several things at once gets
// several 401s at the same moment. Each used to start its own refresh with the
// same refresh token. The server rotates the token on the first use and treats a
// second use as theft, so it revoked every session and the user was thrown out to
// the login page. Now requests that fail together wait for one shared refresh.
let refreshing: Promise<string> | null = null

function refreshOnce(): Promise<string> {
  if (!refreshing) {
    // The browser sends the cookie by itself (same site). A token left over from an
    // older version goes in the body this one time.
    const legacy = localStorage.getItem(LEGACY_REFRESH_KEY)
    refreshing = axios
      .post<{ accessToken: string }>(`${BASE_URL}/auth/refresh`, legacy ? { refreshToken: legacy } : {})
      .then(({ data }) => {
        localStorage.setItem(TOKEN_KEY, data.accessToken)
        localStorage.removeItem(LEGACY_REFRESH_KEY)
        return data.accessToken
      })
      .finally(() => { refreshing = null })
  }
  return refreshing
}

// ---- Response interceptor: normalize errors ---------------------------------

apiClient.interceptors.response.use(
  response => {
    finishTracking(response.config as TrackedConfig)
    return response
  },
  async (error: AxiosError) => {
    finishTracking(error.config as TrackedConfig)
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean }

    // A 401 from the login or register call means the details were wrong, not that
    // the session ran out, so the form should show it as an error. These used to go
    // through the redirect below, which reloaded the login page and wiped the message.
    const isAuthRequest = (originalRequest?.url ?? '').startsWith('/auth/')

    // 401 + not already retried → try refresh
    if (error.response?.status === 401 && !originalRequest._retry && !isAuthRequest) {
      // Signed in (an access token, or a left-over refresh token): try the cookie
      if (getAccessToken() || localStorage.getItem(LEGACY_REFRESH_KEY)) {
        originalRequest._retry = true
        try {
          const accessToken = await refreshOnce()
          originalRequest.headers.Authorization = `Bearer ${accessToken}`
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

    if (isServerUnreachable(error.response?.status)) {
      apiError.title = 'Server not reachable'
      apiError.detail = SERVER_UNREACHABLE_MESSAGE
      return Promise.reject(apiError)
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

// Only the access token is kept; the refresh token arrived as an HttpOnly cookie
export function setTokens(access: string) {
  localStorage.setItem(TOKEN_KEY, access)
  localStorage.removeItem(LEGACY_REFRESH_KEY)
}

export function clearTokens() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(LEGACY_REFRESH_KEY)
}

// A refresh token left in storage by an older version of the app, if any
export function getLegacyRefreshToken(): string | null {
  return localStorage.getItem(LEGACY_REFRESH_KEY)
}

export function getAccessToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export default apiClient
