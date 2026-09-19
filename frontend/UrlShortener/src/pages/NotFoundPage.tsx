import { Link } from 'react-router-dom'
import { ArrowLeft, MapPinOff } from 'lucide-react'
import { Button } from '@/components/ui/Button'
import { ROUTES } from '@/lib/constants'

export function NotFoundPage() { return <main className="flex min-h-dvh items-center justify-center bg-paper p-4 text-center"><div><MapPinOff className="mx-auto h-10 w-10 text-signal" /><p className="mt-5 text-sm font-semibold text-signal">404</p><h1 className="mt-1 text-3xl font-bold text-ink">This link leads nowhere.</h1><p className="mt-2 text-sm text-muted">The page you requested is not part of this workspace.</p><Link to={ROUTES.root} className="mt-6 inline-block"><Button><ArrowLeft className="h-4 w-4" />Return to URL Shortener</Button></Link></div></main> }
