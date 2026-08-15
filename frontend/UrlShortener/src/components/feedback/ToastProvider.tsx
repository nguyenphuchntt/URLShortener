import { createContext, useCallback, useContext, useState, type PropsWithChildren } from 'react'
import { CheckCircle2, XCircle, X } from 'lucide-react'

interface Toast { id: number; message: string; tone: 'success' | 'error' }
interface ToastContextValue { success: (message: string) => void; error: (message: string) => void }
const ToastContext = createContext<ToastContextValue | null>(null)

export function useToast() {
  const value = useContext(ToastContext)
  if (!value) throw new Error('useToast must be used within ToastProvider')
  return value
}

export function ToastProvider({ children }: PropsWithChildren) {
  const [toasts, setToasts] = useState<Toast[]>([])
  const add = useCallback((message: string, tone: Toast['tone']) => {
    const id = Date.now() + Math.floor(Math.random() * 1000)
    setToasts((current) => [...current.slice(-3), { id, message, tone }])
    window.setTimeout(() => setToasts((current) => current.filter((toast) => toast.id !== id)), 4200)
  }, [])
  const dismiss = (id: number) => setToasts((current) => current.filter((toast) => toast.id !== id))
  return <ToastContext.Provider value={{ success: (m) => add(m, 'success'), error: (m) => add(m, 'error') }}>
    {children}
    <div className="fixed right-4 bottom-4 z-50 flex w-[min(24rem,calc(100vw-2rem))] flex-col gap-2" aria-live="polite">
      {toasts.map((toast) => <div key={toast.id} className={`flex items-center gap-3 rounded-lg border bg-white px-4 py-3 shadow-lg ${toast.tone === 'success' ? 'border-emerald-200' : 'border-red-200'}`} role="status">
        {toast.tone === 'success' ? <CheckCircle2 className="h-5 w-5 shrink-0 text-success" /> : <XCircle className="h-5 w-5 shrink-0 text-danger" />}
        <p className="flex-1 text-sm font-medium text-ink">{toast.message}</p>
        <button type="button" onClick={() => dismiss(toast.id)} className="text-muted hover:text-ink" aria-label="Dismiss notification"><X className="h-4 w-4" /></button>
      </div>)}
    </div>
  </ToastContext.Provider>
}
