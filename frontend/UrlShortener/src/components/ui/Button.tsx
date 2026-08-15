import type { ButtonHTMLAttributes, PropsWithChildren } from 'react'
import { cn } from '@/lib/utils'

type ButtonVariant = 'primary' | 'secondary' | 'danger' | 'ghost'
interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> { variant?: ButtonVariant; loading?: boolean }

export function Button({ children, className, variant = 'primary', loading, disabled, ...props }: PropsWithChildren<ButtonProps>) {
  const variants: Record<ButtonVariant, string> = {
    primary: 'bg-signal text-white hover:bg-signal-dark', secondary: 'border border-line bg-white text-ink hover:bg-slate-50', danger: 'bg-danger text-white hover:bg-red-800', ghost: 'text-muted hover:bg-slate-100 hover:text-ink',
  }
  return <button {...props} disabled={disabled || loading} className={cn('inline-flex min-h-10 items-center justify-center gap-2 rounded-lg px-4 text-sm font-semibold transition-colors disabled:opacity-55', variants[variant], className)}>{loading && <span className="h-4 w-4 animate-spin rounded-full border-2 border-current border-r-transparent" />} {children}</button>
}
