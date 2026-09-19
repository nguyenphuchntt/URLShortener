import { type ReactNode } from 'react'
import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../AuthContext'
import { ROUTES } from '@/lib/constants'

export function ProtectedRoute() {
  const { isAuthenticated, isInitializing } = useAuth()
  const location = useLocation()
  if (isInitializing) return <LoadingScreen />
  if (!isAuthenticated) return <Navigate to={ROUTES.login} state={{ from: location }} replace />
  return <Outlet />
}

export function PublicOnlyRoute() {
  const { isAuthenticated, isInitializing } = useAuth()
  if (isInitializing) return <LoadingScreen />
  if (isAuthenticated) return <Navigate to={ROUTES.dashboard} replace />
  return <Outlet />
}

export function AdminRoute({ children }: { children?: ReactNode }) {
  const { user, isInitializing } = useAuth()
  if (isInitializing) return <LoadingScreen />

  const isAdmin = user?.role === 'ADMIN'
  if (!isAdmin) return <Navigate to={ROUTES.dashboard} replace />

  if (children) return <>{children}</>
  return <Outlet />
}

function LoadingScreen() {
  return (
    <div className="flex min-h-dvh items-center justify-center bg-white">
      <div className="flex flex-col items-center gap-3">
        <div className="h-8 w-8 animate-spin rounded-full border-2 border-signal border-r-transparent" />
        <p className="text-sm text-muted">Loading…</p>
      </div>
    </div>
  )
}