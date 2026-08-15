import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../AuthContext'
import { ROUTES } from '@/lib/constants'

export function ProtectedRoute() {
  const { isAuthenticated, isInitializing } = useAuth(); const location = useLocation()
  if (isInitializing) return <div className="flex min-h-dvh items-center justify-center text-sm text-muted">Loading workspace…</div>
  return isAuthenticated ? <Outlet /> : <Navigate to={ROUTES.login} replace state={{ from: location }} />
}

export function PublicOnlyRoute() {
  const { isAuthenticated, isInitializing } = useAuth()
  if (isInitializing) return <div className="flex min-h-dvh items-center justify-center text-sm text-muted">Loading…</div>
  return isAuthenticated ? <Navigate to={ROUTES.dashboard} replace /> : <Outlet />
}
