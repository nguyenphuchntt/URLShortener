import { useAuth } from '@/features/auth/AuthContext'
import { User, Mail, Calendar, Shield } from 'lucide-react'
import { formatDate } from '@/lib/utils'

export function ProfilePage() {
  const { user } = useAuth()

  if (!user) return null

  return (
    <div className="space-y-6">
      <section>
        <h1 className="mt-1 text-3xl font-bold tracking-tight text-ink">Profile</h1>
      </section>

      <div className="max-w-2xl">
        <div className="rounded-xl border border-line bg-white p-6">
          <h2 className="text-lg font-semibold text-ink">Account information</h2>
          <div className="mt-6 space-y-5">
            <Field icon={User} label="Username" value={user.username} />
            <Field icon={Mail} label="Email" value={user.email} />
            <Field
              icon={Calendar}
              label="Member since"
              value={formatDate(user.createdAt)}
            />
            <Field
              icon={Shield}
              label="Role"
              value={user.role || 'User'}
            />
          </div>
        </div>
      </div>
    </div>
  )
}

function Field({
  icon: Icon,
  label,
  value,
}: {
  icon: React.ElementType
  label: string
  value: string
}) {
  return (
    <div className="flex items-start gap-3">
      <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-slate-100 text-muted">
        <Icon className="h-4 w-4" />
      </div>
      <div className="min-w-0 flex-1">
        <p className="text-xs font-semibold uppercase tracking-wide text-muted">
          {label}
        </p>
        <p className="mt-1 truncate text-sm font-medium text-ink">{value}</p>
      </div>
    </div>
  )
}
