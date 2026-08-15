import { useState } from 'react'
import { LogOut, Menu, X } from 'lucide-react'
import { useAuth } from '@/features/auth/AuthContext'
import { useToast } from '@/components/feedback/ToastProvider'
import { getUserLabel } from '@/lib/utils'
import { SidebarNav } from './SidebarNav'

export function AppHeader() {
  const [open, setOpen] = useState(false); const { user, logout } = useAuth(); const { success } = useToast(); const [pending, setPending] = useState(false)
  const signOut = async () => { setPending(true); try { await logout(); success('You have been signed out.') } finally { setPending(false) } }
  return <><header className="flex h-16 items-center justify-between border-b border-line bg-white px-4 lg:justify-end lg:px-8"><button type="button" onClick={() => setOpen(true)} aria-label="Open navigation" aria-expanded={open} className="rounded-lg p-2 text-muted hover:bg-slate-100 hover:text-ink lg:hidden"><Menu className="h-5 w-5" /></button><div className="flex items-center gap-3"><div className="hidden text-right sm:block"><p className="text-sm font-semibold text-ink">{user ? getUserLabel(user.username) : 'Account'}</p><p className="text-xs text-muted">{user?.email}</p></div><span className="flex h-9 w-9 items-center justify-center rounded-full bg-blue-100 text-sm font-bold text-signal">{user?.username.slice(0, 1).toUpperCase()}</span><button type="button" disabled={pending} onClick={signOut} className="rounded-lg p-2 text-muted hover:bg-slate-100 hover:text-ink" aria-label="Sign out"><LogOut className="h-4 w-4" /></button></div></header>{open && <div className="fixed inset-0 z-30 bg-slate-950/35 lg:hidden" onMouseDown={() => setOpen(false)}><div className="h-full w-72 bg-white shadow-xl" onMouseDown={(event) => event.stopPropagation()}><div className="absolute left-60 top-4"><button type="button" aria-label="Close navigation" onClick={() => setOpen(false)} className="rounded-lg bg-white p-2 text-ink shadow"><X className="h-5 w-5" /></button></div><SidebarNav onNavigate={() => setOpen(false)} /></div></div>}</>
}
