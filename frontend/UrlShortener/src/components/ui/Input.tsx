import { forwardRef, type InputHTMLAttributes } from 'react'
import { cn } from '@/lib/utils'

interface InputProps extends InputHTMLAttributes<HTMLInputElement> { error?: string }
export const Input = forwardRef<HTMLInputElement, InputProps>(function Input({ className, error, ...props }, ref) {
  return <input ref={ref} {...props} aria-invalid={Boolean(error) || props['aria-invalid']} className={cn('min-h-11 w-full rounded-lg border bg-white px-3 text-sm text-ink shadow-sm outline-none transition focus:border-signal', error ? 'border-danger' : 'border-line', className)} />
})
