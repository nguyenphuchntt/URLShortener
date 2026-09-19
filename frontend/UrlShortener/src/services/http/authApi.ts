import type { AuthApi, AuthResponse, LoginRequest, RegisterRequest, User } from '@/features/auth/types'
import { http, TOKEN_KEY } from './client'
import { writeStorage, removeStorage } from '@/lib/storage'

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
    const profile = await http.get<BackendUserProfile>('/api/v1/users/me')
    return { user: toUser(profile), accessToken: jwtResponse.accessToken }
  },

  async register(request: RegisterRequest): Promise<AuthResponse> {
    const registerResponse = await http.post<BackendRegisterResponse>('/api/v1/auth/register', {
      username: request.username,
      email: request.email,
      password: request.password,
    })
    writeStorage(TOKEN_KEY, registerResponse.accessToken)
    const profile = await http.get<BackendUserProfile>('/api/v1/users/me')
    return { user: toUser(profile), accessToken: registerResponse.accessToken }
  },

  async logout(): Promise<void> {
    try {
      await http.post<void>('/api/v1/auth/logout', {})
    } finally {
      removeStorage(TOKEN_KEY)
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
