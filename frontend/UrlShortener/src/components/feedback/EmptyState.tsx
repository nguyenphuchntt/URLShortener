import type { ReactNode } from 'react'

export function EmptyState({ icon, title, description, action }: { icon: ReactNode; title: string; description: string; action?: ReactNode }) {
  return <div className="rounded-xl border border-dashed border-line bg-white px-6 py-12 text-center"><div className="mx-auto mb-3 flex h-11 w-11 items-center justify-center rounded-full bg-blue-50 text-signal">{icon}</div><h3 className="font-semibold text-ink">{title}</h3><p className="mx-auto mt-1 max-w-md text-sm text-muted">{description}</p>{action && <div className="mt-5">{action}</div>}</div>
}
