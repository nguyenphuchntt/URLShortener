import type { ApiError } from '@/types/api'
import { readStorage, removeStorage, writeStorage } from '@/lib/storage'

// In Docker, nginx proxies /api to backend, so we use relative path
// In dev, use VITE_API_BASE_URL or default to localhost:8080
const BASE_URL = (import.meta as unknown as { env: Record<string, string> }).env.VITE_API_BASE_URL || ''
const TOKEN_KEY = 'auth_token'
const REFRESH_TOKEN_KEY = 'refresh_token'
const REFRESH_ENDPOINT = '/api/v1/auth/refresh'

// Endpoints that must never trigger the refresh-and-retry flow: a 401 from them means the
// credentials themselves are bad, not that the access token merely expired.
const NO_REFRESH_ENDPOINTS = [REFRESH_ENDPOINT, '/api/v1/auth/login', '/api/v1/auth/register']

class HttpError extends Error implements ApiError {
  code: string
  status?: number
  fieldErrors?: Record<string, string>

  constructor(code: string, message: string, status?: number, fieldErrors?: Record<string, string>) {
    super(message)
    this.name = 'HttpError'
    this.code = code
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

export function clearSession() {
  removeStorage(TOKEN_KEY)
  removeStorage(REFRESH_TOKEN_KEY)
}

export function hasSession() {
  return Boolean(readStorage<string>(TOKEN_KEY, '') || readStorage<string>(REFRESH_TOKEN_KEY, ''))
}

// Single-flight lock: when several requests hit a 401 at once they all await the same refresh
// instead of racing to mint (and overwrite) new tokens.
let refreshInFlight: Promise<string> | null = null

async function refreshAccessToken(): Promise<string> {
  if (refreshInFlight) return refreshInFlight

  refreshInFlight = (async () => {
    const refreshToken = readStorage<string>(REFRESH_TOKEN_KEY, '')
    if (!refreshToken) throw new HttpError('UNAUTHENTICATED', 'No refresh token available', 401)

    const response = await fetch(`${BASE_URL}${REFRESH_ENDPOINT}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    })

    if (!response.ok) {
      clearSession()
      throw new HttpError('SESSION_EXPIRED', 'Session expired', response.status)
    }

    const data = (await response.json()) as { accessToken: string; refreshToken?: string }
    writeStorage(TOKEN_KEY, data.accessToken)
    // Backend currently echoes the same refresh token, but persist a rotated one if it ever sends it.
    if (data.refreshToken) writeStorage(REFRESH_TOKEN_KEY, data.refreshToken)
    return data.accessToken
  })()

  try {
    return await refreshInFlight
  } finally {
    refreshInFlight = null
  }
}

function withAuth(options: RequestInit, token: string): Record<string, string> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...((options.headers as Record<string, string>) || {}),
  }
  if (token && !headers['Authorization']) {
    headers['Authorization'] = `Bearer ${token}`
  }
  return headers
}

async function send(endpoint: string, options: RequestInit, token: string): Promise<Response> {
  const response = await fetch(`${BASE_URL}${endpoint}`, { ...options, headers: withAuth(options, token) })
  return response
}

async function request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const canRefresh = !NO_REFRESH_ENDPOINTS.some((e) => endpoint.startsWith(e))

  let response = await send(endpoint, options, readStorage<string>(TOKEN_KEY, ''))

  if (response.status === 401 && canRefresh) {
    try {
      const freshToken = await refreshAccessToken()
      response = await send(endpoint, options, freshToken)
    } catch {
      // Refresh failed: the session is genuinely over. clearSession already ran inside.
      throw new HttpError('SESSION_EXPIRED', 'Session expired, please log in again', 401)
    }
  }

  if (response.status === 401) {
    clearSession()
  }

  if (response.status === 204) {
    return undefined as T
  }

  const text = await response.text()
  const data = text ? JSON.parse(text) : null

  if (!response.ok) {
    const code = data?.errorCode?.toString() || data?.code || 'UNKNOWN_ERROR'
    const message = data?.errorMessage || data?.message || 'An error occurred'
    const fieldErrors = data?.fieldErrors
    throw new HttpError(code, message, response.status, fieldErrors)
  }

  return data
}

export const http = {
  get: <T>(endpoint: string, options?: RequestInit) => request<T>(endpoint, { ...options, method: 'GET' }),
  post: <T>(endpoint: string, body?: unknown, options?: RequestInit) =>
    request<T>(endpoint, { ...options, method: 'POST', body: body ? JSON.stringify(body) : undefined }),
  patch: <T>(endpoint: string, body?: unknown, options?: RequestInit) =>
    request<T>(endpoint, { ...options, method: 'PATCH', body: body ? JSON.stringify(body) : undefined }),
  delete: <T>(endpoint: string, options?: RequestInit) => request<T>(endpoint, { ...options, method: 'DELETE' }),
}

export { TOKEN_KEY, REFRESH_TOKEN_KEY }
