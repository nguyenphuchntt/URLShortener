import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import type { TimeSeriesPoint } from '../types'
import { formatChartDate, formatNumber } from '@/lib/utils'

export function ClickAnalyticsChart({ data, loading }: { data: TimeSeriesPoint[] | null; loading: boolean }) {
  if (loading) return <div className="h-75 animate-pulse rounded-lg bg-slate-100" />
  if (!data?.length) return <div className="flex h-75 items-center justify-center text-sm text-muted">No click activity is available for this period.</div>
  const total = data.reduce((sum, point) => sum + point.clicks, 0)
  return <><div className="h-75" role="img" aria-label={`${formatNumber(total)} clicks across the last 30 days`}><ResponsiveContainer width="100%" height="100%"><AreaChart data={data} margin={{ top: 8, right: 8, bottom: 0, left: -18 }}><defs><linearGradient id="clicks-area" x1="0" x2="0" y1="0" y2="1"><stop offset="0%" stopColor="#315be7" stopOpacity={0.22} /><stop offset="100%" stopColor="#315be7" stopOpacity={0.01} /></linearGradient></defs><CartesianGrid vertical={false} stroke="#e6eaf0" strokeDasharray="3 4" /><XAxis dataKey="date" axisLine={false} tickLine={false} tick={{ fill: '#61708a', fontSize: 11 }} tickFormatter={formatChartDate} minTickGap={26} /><YAxis axisLine={false} tickLine={false} tick={{ fill: '#61708a', fontSize: 11 }} width={38} /><Tooltip cursor={{ stroke: '#315be7', strokeDasharray: '3 3' }} contentStyle={{ borderRadius: 8, border: '1px solid #dfe5ee', boxShadow: '0 8px 18px rgba(21,32,51,.1)' }} labelFormatter={(label) => formatChartDate(String(label))} formatter={(value) => [formatNumber(Number(value)), 'Clicks']} /><Area type="monotone" dataKey="clicks" stroke="#315be7" strokeWidth={2} fill="url(#clicks-area)" activeDot={{ r: 4, strokeWidth: 2, stroke: '#fff' }} /></AreaChart></ResponsiveContainer></div><p className="sr-only">{formatNumber(total)} clicks across the last 30 days.</p></>
}
