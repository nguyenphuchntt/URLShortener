import { useEffect, useState } from 'react'
import { Shield, Search, ChevronLeft, ChevronRight } from 'lucide-react'
import { Button } from '@/components/ui/Button'
import { InlineError } from '@/components/feedback/InlineError'
import { Dialog } from '@/components/ui/Dialog'
import { useToast } from '@/components/feedback/ToastProvider'
import { toUserMessage, formatDate } from '@/lib/utils'
import { DEFAULT_PAGE_SIZE } from '@/lib/constants'

interface AdminUser {
  id: number
  username: string
  email: string
  enabled: boolean
  role: string
  createdAt: string
}

interface PageResponse<T> {
  content: T[]
  number: number
  size: number
  totalElements: number
  totalPages: number
}

const ADMIN_TOKEN_KEY = 'auth_token'

async function apiRequest<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const token = localStorage.getItem(ADMIN_TOKEN_KEY)
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...((options.headers as Record<string, string>) || {}),
  }
  if (token) headers['Authorization'] = `Bearer ${token}`

  const baseUrl = (import.meta as unknown as { env: Record<string, string> }).env.VITE_API_BASE_URL || ''
  const response = await fetch(`${baseUrl}${endpoint}`, { ...options, headers })
  if (!response.ok) {
    const data = await response.json().catch(() => null)
    throw new Error(data?.errorMessage || data?.message || 'Request failed')
  }
  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

export function AdminPage() {
  const { success, error: toastError } = useToast()
  const [users, setUsers] = useState<AdminUser[]>([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(1)
  const [search, setSearch] = useState('')
  const [selectedUser, setSelectedUser] = useState<AdminUser | null>(null)
  const [statusPending, setStatusPending] = useState(false)

  const loadUsers = async () => {
    setLoading(true)
    setLoadError('')
    try {
      const query = new URLSearchParams({ page: String(page), size: String(DEFAULT_PAGE_SIZE), sort: 'createdAt,desc' })
      if (search) query.set('search', search)
      const result = await apiRequest<PageResponse<AdminUser>>(`/api/v1/admin/users?${query.toString()}`)
      setUsers(result.content)
      setTotalPages(result.totalPages)
    } catch (caught) {
      setLoadError(toUserMessage(caught, 'Unable to load users.'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { void loadUsers() }, [page])

  const toggleUserStatus = async (user: AdminUser) => {
    setStatusPending(true)
    try {
      const updated = await apiRequest<AdminUser>(`/api/v1/admin/users/${user.id}/status`, {
        method: 'PATCH',
        body: JSON.stringify({ enabled: !user.enabled }),
      })
      setUsers((prev) => prev.map((u) => (u.id === updated.id ? updated : u)))
      setSelectedUser(updated)
      success(`User ${updated.enabled ? 'enabled' : 'disabled'} successfully.`)
    } catch (caught) {
      toastError(toUserMessage(caught, 'Could not update user status.'))
    } finally {
      setStatusPending(false)
    }
  }

  const deleteUser = async (userId: number) => {
    if (!window.confirm('Are you sure you want to delete this user? This action cannot be undone.')) return
    try {
      await apiRequest<void>(`/api/v1/admin/users/${userId}`, { method: 'DELETE' })
      setUsers((prev) => prev.filter((u) => u.id !== userId))
      setSelectedUser(null)
      success('User deleted successfully.')
    } catch (caught) {
      toastError(toUserMessage(caught, 'Could not delete user.'))
    }
  }

  return (
    <div className="space-y-6">
      <section>
        <p className="text-sm font-semibold text-signal">Administration</p>
        <h1 className="mt-1 text-3xl font-bold tracking-tight text-ink">Manage users</h1>
        <p className="mt-2 text-sm text-muted">
          View, enable, disable, or delete user accounts.
        </p>
      </section>

      <div className="flex gap-3">
        <label className="relative min-w-0 flex-1">
          <span className="sr-only">Search users</span>
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted" />
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            onKeyDown={(e) => { if (e.key === 'Enter') { setPage(0); void loadUsers() } }}
            placeholder="Search by username or email"
            className="min-h-10 w-full rounded-lg border border-line pl-9 pr-3 text-sm outline-none focus:border-signal"
          />
        </label>
        <Button variant="secondary" onClick={() => { setPage(0); void loadUsers() }}>
          Search
        </Button>
      </div>

      {loadError ? (
        <InlineError message={loadError} onRetry={() => void loadUsers()} />
      ) : loading && users.length === 0 ? (
        <div className="h-80 animate-pulse rounded-xl bg-slate-200" />
      ) : (
        <>
          <div className="overflow-hidden rounded-xl border border-line bg-white">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50 text-xs uppercase tracking-wide text-muted">
                <tr>
                  <th className="px-5 py-3 font-semibold">User</th>
                  <th className="px-4 py-3 font-semibold">Role</th>
                  <th className="px-4 py-3 font-semibold">Status</th>
                  <th className="px-4 py-3 font-semibold">Joined</th>
                  <th className="px-5 py-3 text-right font-semibold">Actions</th>
                </tr>
              </thead>
              <tbody>
                {users.map((user) => (
                  <tr key={user.id} className="border-t border-line">
                    <td className="px-5 py-4">
                      <button
                        type="button"
                        onClick={() => setSelectedUser(user)}
                        className="font-semibold text-ink hover:underline text-left"
                      >
                        {user.username}
                      </button>
                      <p className="mt-0.5 text-xs text-muted">{user.email}</p>
                    </td>
                    <td className="px-4 py-4">
                      <span className="inline-flex items-center gap-1 rounded-full bg-purple-50 px-2.5 py-0.5 text-xs font-medium text-purple-700">
                        <Shield className="h-3 w-3" />
                        {user.role}
                      </span>
                    </td>
                    <td className="px-4 py-4">
                      <span
                        className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium ${
                          user.enabled
                            ? 'bg-green-50 text-green-700 before:h-1.5 before:w-1.5 before:rounded-full before:bg-green-500'
                            : 'bg-red-50 text-red-700 before:h-1.5 before:w-1.5 before:rounded-full before:bg-red-500'
                        }`}
                      >
                        {user.enabled ? 'Active' : 'Disabled'}
                      </span>
                    </td>
                    <td className="px-4 py-4 text-muted">{formatDate(user.createdAt)}</td>
                    <td className="px-5 py-4">
                      <div className="flex justify-end gap-2">
                        <Button
                          variant="secondary"
                          className="min-h-8 px-2 text-xs"
                          onClick={() => setSelectedUser(user)}
                        >
                          View
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {totalPages > 1 && (
            <div className="flex items-center justify-between text-sm">
              <p className="text-muted">
                Page {page + 1} of {totalPages}
              </p>
              <div className="flex gap-2">
                <Button
                  variant="secondary"
                  className="min-h-8 px-3"
                  disabled={page === 0}
                  onClick={() => setPage(page - 1)}
                >
                  <ChevronLeft className="h-4 w-4" />
                  Previous
                </Button>
                <Button
                  variant="secondary"
                  className="min-h-8 px-3"
                  disabled={page >= totalPages - 1}
                  onClick={() => setPage(page + 1)}
                >
                  Next
                  <ChevronRight className="h-4 w-4" />
                </Button>
              </div>
            </div>
          )}
        </>
      )}

      <UserDetailDialog
        user={selectedUser}
        pending={statusPending}
        onToggleStatus={() => selectedUser && void toggleUserStatus(selectedUser)}
        onDelete={() => selectedUser && void deleteUser(selectedUser.id)}
        onClose={() => setSelectedUser(null)}
      />
    </div>
  )
}

function UserDetailDialog({
  user,
  pending,
  onToggleStatus,
  onDelete,
  onClose,
}: {
  user: AdminUser | null
  pending: boolean
  onToggleStatus: () => void
  onDelete: () => void
  onClose: () => void
}) {
  if (!user) return null
  return (
    <Dialog
      open={Boolean(user)}
      onClose={onClose}
      title={user.username}
      description={`Member since ${formatDate(user.createdAt)}`}
    >
      <div className="space-y-4">
        <div className="rounded-lg bg-slate-50 p-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <p className="text-xs font-semibold uppercase tracking-wide text-muted">
                Email
              </p>
              <p className="mt-1 text-sm text-ink">{user.email}</p>
            </div>
            <div>
              <p className="text-xs font-semibold uppercase tracking-wide text-muted">
                Role
              </p>
              <p className="mt-1 text-sm text-ink">{user.role}</p>
            </div>
            <div>
              <p className="text-xs font-semibold uppercase tracking-wide text-muted">
                Status
              </p>
              <p className="mt-1 text-sm text-ink">
                {user.enabled ? 'Active' : 'Disabled'}
              </p>
            </div>
            <div>
              <p className="text-xs font-semibold uppercase tracking-wide text-muted">
                User ID
              </p>
              <p className="mt-1 text-sm text-ink">{user.id}</p>
            </div>
          </div>
        </div>
        <div className="flex justify-end gap-3">
          <Button variant="secondary" onClick={onToggleStatus} loading={pending}>
            {user.enabled ? 'Disable user' : 'Enable user'}
          </Button>
          <Button variant="danger" onClick={onDelete} disabled={pending}>
            Delete user
          </Button>
        </div>
      </div>
    </Dialog>
  )
}