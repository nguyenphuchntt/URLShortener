import { NavLink } from 'react-router-dom'
import { BarChart3, Link2, User, Shield } from 'lucide-react'
import { APP_NAME, ROUTES } from '@/lib/constants'
import { useAuth } from '@/features/auth/AuthContext'

const NAV_ITEMS = [
  { to: ROUTES.dashboard, label: 'Dashboard', icon: BarChart3 },
  { to: ROUTES.links, label: 'My links', icon: Link2 },
  { to: ROUTES.profile, label: 'Profile', icon: User },
  { to: ROUTES.admin, label: 'Admin', icon: Shield, adminOnly: true },
]

export function SidebarNav({ onNavigate }: { onNavigate?: () => void }) {
  const { user } = useAuth()
  const isAdmin = user?.role === 'ADMIN'

  return (
    <aside className="flex h-full w-64 shrink-0 flex-col border-r border-line bg-white px-4 py-6">
      <div className="mb-10 flex items-center gap-2 px-2 text-lg font-bold text-ink">
        <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-signal text-white">
          <Link2 className="h-5 w-5" />
        </span>
        {APP_NAME}
      </div>
      <nav aria-label="Main navigation" className="space-y-1">
        {NAV_ITEMS.filter(item => !item.adminOnly || isAdmin).map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            onClick={onNavigate}
            className={({ isActive }: { isActive: boolean }) =>
              `flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-semibold ${
                isActive ? 'bg-blue-50 text-signal' : 'text-muted hover:bg-slate-50 hover:text-ink'
              }`
            }
          >
            <Icon className="h-4 w-4" />
            {label}
          </NavLink>
        ))}
      </nav>
    </aside>
  )
}
