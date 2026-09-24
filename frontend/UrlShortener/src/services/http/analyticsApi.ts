import type {
  AnalyticsApi,
  AnalyticsParams,
  CountryClicksPoint,
  DashboardOverview,
  HourlyClicksPoint,
  LinkSummary,
  TimeSeriesPoint,
} from '@/features/dashboard/types'
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

interface HourlyResponse {
  shortUrlId: number
  items: HourlyClicksPoint[]
}

interface CountryResponse {
  shortUrlId: number
  items: CountryClicksPoint[]
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

  async getLinkSummary(shortUrlId: number): Promise<LinkSummary> {
    return http.get<LinkSummary>(`/api/v1/analytics/${shortUrlId}/summary`)
  },

  async getHourlyClicks(shortUrlId: number, hours = 24): Promise<HourlyClicksPoint[]> {
    const result = await http.get<HourlyResponse>(
      `/api/v1/analytics/${shortUrlId}/hourly?hours=${hours}`,
    )
    return result.items
  },

  async getClicksByCountry(shortUrlId: number, days = 30): Promise<CountryClicksPoint[]> {
    const result = await http.get<CountryResponse>(
      `/api/v1/analytics/${shortUrlId}/by-country?days=${days}`,
    )
    return result.items
  },
}
