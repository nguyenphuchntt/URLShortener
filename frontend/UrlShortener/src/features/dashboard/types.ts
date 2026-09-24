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

export interface LinkSummary {
  shortUrlId: number
  shortCode: string
  totalClicks: number
  lastClickAt: string | null
}

export interface HourlyClicksPoint {
  hour: string
  clicks: number
}

export interface CountryClicksPoint {
  country: string
  clicks: number
}

export interface AnalyticsApi {
  getOverview(): Promise<DashboardOverview>
  getClickTimeseries(params: AnalyticsParams): Promise<TimeSeriesPoint[]>
  getLinkSummary(shortUrlId: number): Promise<LinkSummary>
  getHourlyClicks(shortUrlId: number, hours?: number): Promise<HourlyClicksPoint[]>
  getClicksByCountry(shortUrlId: number, days?: number): Promise<CountryClicksPoint[]>
}
