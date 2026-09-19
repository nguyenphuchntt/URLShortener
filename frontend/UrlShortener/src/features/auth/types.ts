export interface User {
  id: string
  username: string
  email: string
  enabled: boolean
  createdAt: string
  role?: string
}

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  email: string
  password: string
}

export interface AuthResponse {
  user: User
  accessToken: string
}

export interface AuthApi {
  login(request: LoginRequest): Promise<AuthResponse>
  register(request: RegisterRequest): Promise<AuthResponse>
  logout(): Promise<void>
  getCurrentUser(): Promise<User | null>
}