import type { ApiError } from '@/types/api'
import { readStorage, removeStorage } from '@/lib/storage'

// In Docker, nginx proxies /api to backend, so we use relative path
// In dev, use VITE_API_BASE_URL or default to localhost:8080
const BASE_URL = (import.meta as unknown as { env: Record<string, string> }).env.VITE_API_BASE_URL || ''
const TOKEN_KEY = 'auth_token'

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

async function request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const token = readStorage<string>(TOKEN_KEY, '')
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...((options.headers as Record<string, string>) || {}),
  }
  if (token && !headers['Authorization']) {
    headers['Authorization'] = `Bearer ${token}`
  }

  const url = `${BASE_URL}${endpoint}`
  const response = await fetch(url, { ...options, headers })

  if (response.status === 401) {
    removeStorage(TOKEN_KEY)
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

export { TOKEN_KEY }
