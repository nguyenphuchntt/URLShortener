import { Link, Outlet } from 'react-router-dom'
import { Link2 } from 'lucide-react'
import { APP_NAME, ROUTES } from '@/lib/constants'

export function AuthLayout() {
  return <main className="flex min-h-dvh items-center justify-center bg-paper px-4 py-8"><div className="w-full max-w-md"><Link to={ROUTES.login} className="mb-8 flex items-center justify-center gap-2 text-lg font-bold text-ink"><span className="flex h-9 w-9 items-center justify-center rounded-lg bg-signal text-white"><Link2 className="h-5 w-5" /></span>{APP_NAME}</Link><section className="rounded-xl border border-line bg-white p-6 shadow-sm sm:p-8"><Outlet /></section><p className="mt-6 text-center text-xs text-muted">Link management made clear.</p></div></main>
}
