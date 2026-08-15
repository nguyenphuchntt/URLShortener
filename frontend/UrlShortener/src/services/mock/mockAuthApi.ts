import type { AuthApi, AuthResponse, LoginRequest, RegisterRequest } from '@/features/auth/types'
import { MockApiError } from './mockError'
import { mockStore } from './mockStore'
import { mockDelay } from './mockDelay'

function toAuthResponse(user: { id: string; username: string; email: string; enabled: boolean; createdAt: string }) {
  return { id: user.id, username: user.username, email: user.email, enabled: user.enabled, createdAt: user.createdAt }
}

export const mockAuthApi: AuthApi = {
  async login(request: LoginRequest): Promise<AuthResponse> {
    await mockDelay()
    const user = mockStore.findUserByEmail(request.email)
    if (!user || user.password !== request.password) {
      throw new MockApiError('INVALID_CREDENTIALS', 'Incorrect email or password.', 401)
    }
    const token = `tok_${user.id}_${Date.now()}`
    mockStore.setSession({ userId: user.id, token })
    return { user: toAuthResponse(user), accessToken: token }
  },

  async register(request: RegisterRequest): Promise<AuthResponse> {
    await mockDelay()
    const existingEmail = mockStore.findUserByEmail(request.email)
    if (existingEmail) {
      throw new MockApiError('EMAIL_ALREADY_EXISTS', 'An account with this email already exists.', 409, { email: 'Email already exists.' })
    }
    const existingUsername = mockStore.findUserByUsername(request.username)
    if (existingUsername) {
      throw new MockApiError('USERNAME_ALREADY_EXISTS', 'This display name is already taken.', 409, { username: 'Display name already exists.' })
    }
    const id = `usr_${String(Date.now()).slice(-6)}${Math.random().toString(16).slice(2, 4)}`
    const user = { id, username: request.username, email: request.email, password: request.password, enabled: true, createdAt: new Date().toISOString() }
    mockStore.addUser(user)
    const token = `tok_${user.id}_${Date.now()}`
    mockStore.setSession({ userId: user.id, token })
    return { user: toAuthResponse(user), accessToken: token }
  },

  async logout(): Promise<void> {
    await mockDelay()
    mockStore.clearSession()
  },

  async getCurrentUser(): Promise<AuthResponse['user'] | null> {
    await mockDelay()
    const session = mockStore.getSession()
    if (!session) return null
    const user = mockStore.getUsers().find((u) => u.id === session.userId)
    if (!user) return null
    return toAuthResponse(user)
  },
}
