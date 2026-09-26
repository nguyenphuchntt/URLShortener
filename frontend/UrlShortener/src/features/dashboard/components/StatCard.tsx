import type { ReactNode } from 'react'

export function StatCard({ label, value, detail, icon }: { label: string; value: string; detail: string; icon: ReactNode }) {
  return <article className="rounded-xl border border-line bg-white p-5 shadow-sm"><div className="flex items-start justify-between gap-3"><p className="text-sm font-medium text-muted">{label}</p><span className="rounded-lg bg-blue-50 p-2 text-signal">{icon}</span></div><p className="mt-5 text-3xl font-bold tracking-tight text-ink">{value}</p><p className="mt-2 text-xs text-muted">{detail}</p></article>
}
