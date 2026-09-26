import type { AuthApi, AuthResponse, LoginRequest, RegisterRequest, User } from '@/features/auth/types'
import { http, TOKEN_KEY, REFRESH_TOKEN_KEY, clearSession } from './client'
import { readStorage, writeStorage } from '@/lib/storage'

interface BackendJwtResponse {
  accessToken: string
  refreshToken: string
}

interface BackendRegisterResponse {
  username: string
  userId: number
  accessToken: string
  refreshToken: string
}

interface BackendUserProfile {
  id: number
  username: string
  email: string
  enabled: boolean
  role: string
  createdAt: string
}

function toUser(profile: BackendUserProfile): User {
  return {
    id: String(profile.id),
    username: profile.username,
    email: profile.email,
    enabled: profile.enabled,
    createdAt: profile.createdAt,
    role: profile.role,
  }
}

export const httpAuthApi: AuthApi = {
  async login(request: LoginRequest): Promise<AuthResponse> {
    const jwtResponse = await http.post<BackendJwtResponse>('/api/v1/auth/login', {
      username: request.username,
      password: request.password,
    })
    writeStorage(TOKEN_KEY, jwtResponse.accessToken)
    writeStorage(REFRESH_TOKEN_KEY, jwtResponse.refreshToken)
    try {
      const profile = await http.get<BackendUserProfile>('/api/v1/users/me')
      return { user: toUser(profile), accessToken: jwtResponse.accessToken }
    } catch (error) {
      // Don't leave orphaned tokens behind if the profile fetch fails right after login.
      clearSession()
      throw error
    }
  },

  async register(request: RegisterRequest): Promise<AuthResponse> {
    const registerResponse = await http.post<BackendRegisterResponse>('/api/v1/auth/register', {
      username: request.username,
      email: request.email,
      password: request.password,
    })
    writeStorage(TOKEN_KEY, registerResponse.accessToken)
    writeStorage(REFRESH_TOKEN_KEY, registerResponse.refreshToken)
    try {
      const profile = await http.get<BackendUserProfile>('/api/v1/users/me')
      return { user: toUser(profile), accessToken: registerResponse.accessToken }
    } catch (error) {
      clearSession()
      throw error
    }
  },

  async logout(): Promise<void> {
    const refreshToken = readStorage<string>(REFRESH_TOKEN_KEY, '')
    try {
      // The backend requires the refresh token in the body to revoke it, and reads the access
      // token from the Authorization header (attached by the http client) to blacklist it.
      if (refreshToken) {
        await http.post<void>('/api/v1/auth/logout', { refreshToken })
      }
    } catch {
      // Even if the server call fails, drop the local session.
    } finally {
      clearSession()
    }
  },

  async getCurrentUser(): Promise<User | null> {
    try {
      const profile = await http.get<BackendUserProfile>('/api/v1/users/me')
      return toUser(profile)
    } catch {
      return null
    }
  },
}
