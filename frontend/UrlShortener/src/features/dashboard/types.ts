export interface DashboardOverview {
  totalLinks: number
  totalClicks: number
  activeLinks: number
  disabledLinks: number
  expiredLinks: number
  deletedLinks: number
}

export interface TimeSeriesPoint {
  date: string
  clicks: number
}

export interface AnalyticsParams {
  from: string
  to: string
  granularity: 'day' | 'week' | 'month'
}

export interface AnalyticsApi {
  getOverview(): Promise<DashboardOverview>
  getClickTimeseries(params: AnalyticsParams): Promise<TimeSeriesPoint[]>
}
