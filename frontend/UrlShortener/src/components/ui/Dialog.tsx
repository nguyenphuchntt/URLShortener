import { useEffect, useRef, type PropsWithChildren } from 'react'
import { X } from 'lucide-react'

interface DialogProps { open: boolean; onClose: () => void; title: string; description?: string; className?: string }
export function Dialog({ open, onClose, title, description, className = '', children }: PropsWithChildren<DialogProps>) {
  const closeRef = useRef<HTMLButtonElement>(null)
  const onCloseRef = useRef(onClose)
  onCloseRef.current = onClose

  useEffect(() => {
    if (!open) return
    closeRef.current?.focus()
    const keydown = (event: KeyboardEvent) => { if (event.key === 'Escape') onCloseRef.current() }
    window.addEventListener('keydown', keydown)
    return () => window.removeEventListener('keydown', keydown)
  }, [open])

  if (!open) return null
  return <div className="fixed inset-0 z-40 flex items-end justify-center bg-slate-950/35 p-4 sm:items-center" role="presentation" onMouseDown={() => onCloseRef.current()}>
    <section role="dialog" aria-modal="true" aria-labelledby="dialog-title" aria-describedby={description ? 'dialog-description' : undefined} onMouseDown={(event) => event.stopPropagation()} className={`w-full max-w-lg rounded-xl bg-white p-6 shadow-2xl ${className}`}>
      <div className="mb-5 flex items-start justify-between gap-4"><div><h2 id="dialog-title" className="text-xl font-bold text-ink">{title}</h2>{description && <p id="dialog-description" className="mt-1 text-sm text-muted">{description}</p>}</div><button ref={closeRef} type="button" onClick={() => onCloseRef.current()} aria-label="Close dialog" className="rounded-md p-1 text-muted hover:bg-slate-100 hover:text-ink"><X className="h-5 w-5" /></button></div>{children}
    </section>
  </div>
}
