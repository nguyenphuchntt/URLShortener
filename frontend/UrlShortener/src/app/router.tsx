import { Navigate, createBrowserRouter } from 'react-router-dom'
import { AuthLayout } from '@/layouts/AuthLayout'
import { DashboardLayout } from '@/layouts/DashboardLayout'
import { LoginPage } from '@/pages/auth/LoginPage'
import { RegisterPage } from '@/pages/auth/RegisterPage'
import { DashboardPage } from '@/pages/DashboardPage'
import { NotFoundPage } from '@/pages/NotFoundPage'
import { ProtectedRoute, PublicOnlyRoute } from '@/features/auth/components/RouteGuards'
import { ROUTES } from '@/lib/constants'

export const router = createBrowserRouter([
  { element: <PublicOnlyRoute />, children: [{ element: <AuthLayout />, children: [{ path: ROUTES.login, element: <LoginPage /> }, { path: ROUTES.register, element: <RegisterPage /> }]}] },
  { element: <ProtectedRoute />, children: [{ element: <DashboardLayout />, children: [{ path: ROUTES.dashboard, element: <DashboardPage /> }]}] },
  { path: ROUTES.root, element: <Navigate to={ROUTES.dashboard} replace /> },
  { path: '*', element: <NotFoundPage /> },
])
