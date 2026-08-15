import type { AnalyticsApi, AnalyticsParams, DashboardOverview, TimeSeriesPoint } from '@/features/dashboard/types'
import { getLinkDisplayStatus } from '@/features/links/types'
import { mockStore } from './mockStore'
import { mockDelay } from './mockDelay'

function computeOverview(): DashboardOverview {
  const links = mockStore.getLinks()
  const now = new Date()
  const counts: Record<string, number> = { ACTIVE: 0, DISABLED: 0, EXPIRED: 0, DELETED: 0 }
  links.forEach((link) => {
    counts[getLinkDisplayStatus(link, now)] = (counts[getLinkDisplayStatus(link, now)] ?? 0) + 1
  })
  return {
    totalLinks: links.length,
    totalClicks: links.reduce((sum, link) => sum + link.clicks, 0),
    activeLinks: counts['ACTIVE'] ?? 0,
    disabledLinks: counts['DISABLED'] ?? 0,
    expiredLinks: counts['EXPIRED'] ?? 0,
    deletedLinks: counts['DELETED'] ?? 0,
  }
}

function toPoint(date: Date): string {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

export const mockAnalyticsApi: AnalyticsApi = {
  async getOverview(): Promise<DashboardOverview> {
    await mockDelay()
    return computeOverview()
  },

  async getClickTimeseries(params: AnalyticsParams): Promise<TimeSeriesPoint[]> {
    await mockDelay()
    const from = new Date(`${params.from}T12:00:00.000Z`)
    const to = new Date(`${params.to}T12:00:00.000Z`)
    if (Number.isNaN(from.getTime()) || Number.isNaN(to.getTime()) || to.getTime() < from.getTime()) return []

    const points: TimeSeriesPoint[] = []
    const current = new Date(from)
    let seed = from.getFullYear() * 1000 + from.getMonth() * 31 + from.getDate()
    while (current.getTime() <= to.getTime()) {
      seed = (seed * 1103515245 + 12345) & 0x7fffffff
      points.push({ date: toPoint(current), clicks: 28 + (seed % 162) })
      current.setDate(current.getDate() + 1)
    }
    return points
  },
}
