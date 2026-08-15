import { NavLink } from 'react-router-dom'
import { BarChart3, Link2 } from 'lucide-react'
import { APP_NAME, ROUTES } from '@/lib/constants'

export function SidebarNav({ onNavigate }: { onNavigate?: () => void }) {
  return <aside className="flex h-full w-64 shrink-0 flex-col border-r border-line bg-white px-4 py-6"><div className="mb-10 flex items-center gap-2 px-2 text-lg font-bold text-ink"><span className="flex h-9 w-9 items-center justify-center rounded-lg bg-signal text-white"><Link2 className="h-5 w-5" /></span>{APP_NAME}</div><nav aria-label="Main navigation" className="space-y-1"><NavLink to={ROUTES.dashboard} onClick={onNavigate} className={({ isActive }: { isActive: boolean }) => `flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-semibold ${isActive ? 'bg-blue-50 text-signal' : 'text-muted hover:bg-slate-50 hover:text-ink'}`}><BarChart3 className="h-4 w-4" />Dashboard</NavLink></nav><div className="mt-auto rounded-lg bg-paper p-3 text-xs leading-relaxed text-muted"><p className="font-semibold text-ink">Keep links moving.</p><p className="mt-1">Shortly keeps the useful parts of link tracking close at hand.</p></div></aside>
}
