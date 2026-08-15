import { AlertCircle } from 'lucide-react'
import { Button } from '@/components/ui/Button'

export function InlineError({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return <div className="flex flex-wrap items-center gap-3 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-danger" role="alert"><AlertCircle className="h-5 w-5" /><span className="flex-1">{message}</span>{onRetry && <Button variant="secondary" className="min-h-8 px-3" onClick={onRetry}>Retry</Button>}</div>
}
