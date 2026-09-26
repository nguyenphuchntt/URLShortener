import type { AuthApi } from '@/features/auth/types'
import type { AnalyticsApi } from '@/features/dashboard/types'
import type { LinksApi } from '@/features/links/types'

export interface ApiServices {
  auth: AuthApi
  links: LinksApi
  analytics: AnalyticsApi
}
