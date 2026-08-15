export const APP_NAME = 'Shortly'
export const SHORT_DOMAIN = 'https://sho.rt'
export const DEFAULT_PAGE_SIZE = 8
export const DEMO_EMAIL = 'demo@example.com'
export const DEMO_PASSWORD = 'password123'

export const ROUTES = {
  root: '/',
  login: '/login',
  register: '/register',
  dashboard: '/dashboard',
} as const

export const STATUS_FILTERS = [
  { value: 'ALL', label: 'All statuses' },
  { value: 'ACTIVE', label: 'Active' },
  { value: 'DISABLED', label: 'Disabled' },
  { value: 'EXPIRED', label: 'Expired' },
  { value: 'DELETED', label: 'Deleted' },
] as const
