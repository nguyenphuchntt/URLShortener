interface StoredUser {
  id: string
  username: string
  email: string
  password: string
  enabled: boolean
  createdAt: string
}

export const seedUsers: StoredUser[] = [
  {
    id: 'usr_demo',
    username: 'Demo User',
    email: 'demo@example.com',
    password: 'password123',
    enabled: true,
    createdAt: '2026-01-14T09:00:00.000Z',
  },
]

export type { StoredUser }
