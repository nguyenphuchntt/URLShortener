export const APP_NAME = 'URL Shortener'
export const SHORT_DOMAIN = 'https://sho.rt'
export const DEFAULT_PAGE_SIZE = 8

export const ROUTES = {
  root: '/',
  login: '/login',
  register: '/register',
  dashboard: '/dashboard',
  links: '/links',
  linksNew: '/links/new',
  profile: '/profile',
  admin: '/admin',
} as const

export const STATUS_FILTERS = [
  { value: 'ALL', label: 'All statuses' },
  { value: 'ACTIVE', label: 'Active' },
  { value: 'DISABLED', label: 'Disabled' },
  { value: 'EXPIRED', label: 'Expired' },
  { value: 'DELETED', label: 'Deleted' },
] as const