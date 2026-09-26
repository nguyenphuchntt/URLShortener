import { createContext, useContext, type PropsWithChildren } from 'react'
import type { ApiServices } from '@/services/api/contracts'
import { api } from '@/services/api'
import { AuthProvider } from '@/features/auth/AuthContext'
import { ToastProvider } from '@/components/feedback/ToastProvider'

const ApiContext = createContext<ApiServices | null>(null)

export function useApi() {
  const value = useContext(ApiContext)
  if (!value) throw new Error('useApi must be used inside AppProviders')
  return value
}

export function AppProviders({ children }: PropsWithChildren) {
  return (
    <ApiContext.Provider value={api}>
      <ToastProvider>
        <AuthProvider>{children}</AuthProvider>
      </ToastProvider>
    </ApiContext.Provider>
  )
}
