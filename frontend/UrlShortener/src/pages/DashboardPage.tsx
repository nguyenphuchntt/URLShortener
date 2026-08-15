import { useEffect, useState } from 'react'
import { Plus } from 'lucide-react'
import { useApi } from '@/app/providers'
import { InlineError } from '@/components/feedback/InlineError'
import { Button } from '@/components/ui/Button'
import type { DashboardOverview, TimeSeriesPoint } from '@/features/dashboard/types'
import { ClickAnalyticsChart } from '@/features/dashboard/components/ClickAnalyticsChart'
import { StatsGrid } from '@/features/dashboard/components/StatsGrid'
import { LinksSection } from '@/features/links/components/LinksSection'
import { CreateLinkDialog } from '@/features/links/components/CreateLinkDialog'
import type { ShortLink } from '@/features/links/types'
import { toUserMessage } from '@/lib/utils'
import { useAuth } from '@/features/auth/AuthContext'

function dateString(date: Date) { return date.toISOString().slice(0, 10) }

export function DashboardPage() {
  const { analytics } = useApi(); const { user } = useAuth(); const [overview, setOverview] = useState<DashboardOverview | null>(null); const [series, setSeries] = useState<TimeSeriesPoint[] | null>(null); const [loading, setLoading] = useState(true); const [error, setError] = useState(''); const [createOpen, setCreateOpen] = useState(false)
  const load = async () => { setLoading(true); setError(''); const end = new Date(); const start = new Date(end); start.setDate(end.getDate() - 29); try { const [overviewResult, seriesResult] = await Promise.all([analytics.getOverview(), analytics.getClickTimeseries({ from: dateString(start), to: dateString(end), granularity: 'day' })]); setOverview(overviewResult); setSeries(seriesResult) } catch (caught) { setError(toUserMessage(caught, 'Unable to load dashboard data.')) } finally { setLoading(false) } }
  useEffect(() => { void load() }, [])
  const created = (_link: ShortLink) => { setCreateOpen(false); void load() }
  return <><section className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between"><div><p className="text-sm font-semibold text-signal">Workspace overview</p><h1 className="mt-1 text-3xl font-bold tracking-tight text-ink">Welcome back, {user?.username.split(/[._ -]/)[0] ?? 'there'}.</h1><p className="mt-2 text-sm text-muted">See where your links are taking people, then act on the details.</p></div><Button onClick={() => setCreateOpen(true)}><Plus className="h-4 w-4" />New link</Button></section>{error ? <div className="mt-6"><InlineError message={error} onRetry={() => void load()} /></div> : <><section className="mt-7"><StatsGrid data={overview} loading={loading} /></section><section className="mt-6 rounded-xl border border-line bg-white p-5 shadow-sm sm:p-6"><div className="mb-5"><h2 className="text-lg font-bold text-ink">Clicks over the last 30 days</h2><p className="mt-1 text-sm text-muted">A daily view of engagement across all your short links.</p></div><ClickAnalyticsChart data={series} loading={loading} /></section><LinksSection onChanged={() => void load()} /></>}<CreateLinkDialog open={createOpen} onClose={() => setCreateOpen(false)} onCreated={created} /></>
}
