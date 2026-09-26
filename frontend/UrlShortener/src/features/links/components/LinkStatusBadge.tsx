import { CircleCheck, CircleOff, Clock3, Trash2 } from 'lucide-react'
import type { LinkDisplayStatus } from '../types'

const styles: Record<LinkDisplayStatus, { text: string; classes: string; icon: typeof CircleCheck }> = {
  ACTIVE: { text: 'Active', classes: 'bg-emerald-50 text-success ring-emerald-100', icon: CircleCheck },
  DISABLED: { text: 'Disabled', classes: 'bg-slate-100 text-slate-700 ring-slate-200', icon: CircleOff },
  EXPIRED: { text: 'Expired', classes: 'bg-amber-50 text-warning ring-amber-100', icon: Clock3 },
  DELETED: { text: 'Deleted', classes: 'bg-red-50 text-danger ring-red-100', icon: Trash2 },
}
export function LinkStatusBadge({ status }: { status: LinkDisplayStatus }) { const item = styles[status]; const Icon = item.icon; return <span className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-semibold ring-1 ${item.classes}`}><Icon className="h-3.5 w-3.5" />{item.text}</span> }
