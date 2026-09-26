import { useState, type InputHTMLAttributes } from 'react'
import { Eye, EyeOff } from 'lucide-react'
import { Input } from '@/components/ui/Input'

interface PasswordInputProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'type'> { error?: string }
export function PasswordInput({ error, ...props }: PasswordInputProps) {
  const [visible, setVisible] = useState(false)
  return <div className="relative"><Input {...props} type={visible ? 'text' : 'password'} error={error} className="pr-11" /><button type="button" onClick={() => setVisible((value) => !value)} aria-label={visible ? 'Hide password' : 'Show password'} className="absolute right-2 top-1/2 -translate-y-1/2 rounded-md p-2 text-muted hover:bg-slate-100 hover:text-ink">{visible ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}</button></div>
}
