import { Link2, MousePointerClick, CircleCheck, CirclePause } from 'lucide-react'
import type { DashboardOverview } from '../types'
import { formatNumber } from '@/lib/utils'
import { StatCard } from './StatCard'

export function StatsGrid({ data, loading }: { data: DashboardOverview | null; loading: boolean }) {
  const cards = data ? [
    ['Total links', formatNumber(data.totalLinks), `${data.expiredLinks} expired · ${data.deletedLinks} deleted`, <Link2 key="links" className="h-5 w-5" />],
    ['Total clicks', formatNumber(data.totalClicks), 'Across all of your links', <MousePointerClick key="clicks" className="h-5 w-5" />],
    ['Active links', formatNumber(data.activeLinks), 'Available to visitors now', <CircleCheck key="active" className="h-5 w-5" />],
    ['Disabled links', formatNumber(data.disabledLinks), 'Paused until you re-enable them', <CirclePause key="disabled" className="h-5 w-5" />],
  ] as const : []
  if (loading) return <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">{Array.from({ length: 4 }, (_, index) => <div key={index} className="h-40 animate-pulse rounded-xl bg-slate-200" />)}</div>
  return <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">{cards.map(([label, value, detail, icon]) => <StatCard key={label} label={label} value={value} detail={detail} icon={icon} />)}</div>
}
