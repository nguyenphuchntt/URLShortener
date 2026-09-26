import type { ApiServices } from '../api/contracts'
import { httpAuthApi } from './authApi'
import { httpLinksApi } from './linksApi'
import { httpAnalyticsApi } from './analyticsApi'

export const httpApi: ApiServices = {
  auth: httpAuthApi,
  links: httpLinksApi,
  analytics: httpAnalyticsApi,
}

export { http, TOKEN_KEY } from './client'
