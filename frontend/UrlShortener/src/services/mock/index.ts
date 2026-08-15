import type { ApiServices } from '../api/contracts'
import { mockAuthApi } from './mockAuthApi'
import { mockLinksApi } from './mockLinksApi'
import { mockAnalyticsApi } from './mockAnalyticsApi'

export const mockApi: ApiServices = {
  auth: mockAuthApi,
  links: mockLinksApi,
  analytics: mockAnalyticsApi,
}
