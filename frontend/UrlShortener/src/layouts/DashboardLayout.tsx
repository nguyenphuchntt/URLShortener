import { Outlet } from 'react-router-dom'
import { SidebarNav } from '@/components/layout/SidebarNav'
import { AppHeader } from '@/components/layout/AppHeader'

export function DashboardLayout() {
  return <div className="flex min-h-dvh bg-paper"><div className="hidden lg:block"><SidebarNav /></div><div className="min-w-0 flex-1"><AppHeader /><main className="mx-auto max-w-[1440px] px-4 py-7 sm:px-6 lg:px-8"><Outlet /></main></div></div>
}
