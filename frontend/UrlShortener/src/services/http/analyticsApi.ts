import type { AnalyticsApi, AnalyticsParams, DashboardOverview, TimeSeriesPoint } from '@/features/dashboard/types'
import { http } from './client'

interface OverviewResponse {
  totalLinks: number
  totalClicks: number
  activeLinks: number
  disabledLinks: number
  expiredLinks: number
  deletedLinks: number
}

interface TimeseriesResponse {
  items: TimeSeriesPoint[]
}

export const httpAnalyticsApi: AnalyticsApi = {
  async getOverview(): Promise<DashboardOverview> {
    return http.get<OverviewResponse>('/api/v1/analytics/overview')
  },

  async getClickTimeseries(params: AnalyticsParams): Promise<TimeSeriesPoint[]> {
    const query = new URLSearchParams({
      from: params.from,
      to: params.to,
      granularity: params.granularity,
    })
    const result = await http.get<TimeseriesResponse>(`/api/v1/analytics/clicks?${query.toString()}`)
    return result.items
  },
}
