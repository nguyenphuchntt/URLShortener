import { useEffect, useState } from 'react'
import { Link, ExternalLink, Copy, Plus, Search, ChevronLeft, ChevronRight, Pencil, Trash2 } from 'lucide-react'
import { useApi } from '@/app/providers'
import { Button } from '@/components/ui/Button'
import { EmptyState } from '@/components/feedback/EmptyState'
import { InlineError } from '@/components/feedback/InlineError'
import { Dialog } from '@/components/ui/Dialog'
import { useToast } from '@/components/feedback/ToastProvider'
import { DEFAULT_PAGE_SIZE, STATUS_FILTERS } from '@/lib/constants'
import { copyToClipboard, formatDate, formatNumber, toUserMessage, truncateUrl } from '@/lib/utils'
import type { PagedResponse, SortOption } from '@/types/api'
import { getLinkDisplayStatus, type LinkDisplayStatus, type ShortLink } from '@/features/links/types'
import { LinkStatusBadge } from '@/features/links/components/LinkStatusBadge'
import { CreateLinkDialog } from '@/features/links/components/CreateLinkDialog'
import { EditLinkDialog } from '@/features/links/components/EditLinkDialog'

export function LinksPage() {
  const { links: linksApi } = useApi()
  const { success, error } = useToast()
  const [result, setResult] = useState<PagedResponse<ShortLink> | null>(null)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [search, setSearch] = useState('')
  const [debouncedSearch, setDebouncedSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState<LinkDisplayStatus | 'ALL'>('ALL')
  const [sort, setSort] = useState<SortOption>('newest')
  const [page, setPage] = useState(0)
  const [createOpen, setCreateOpen] = useState(false)
  const [editTarget, setEditTarget] = useState<ShortLink | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<ShortLink | null>(null)
  const [mutationPending, setMutationPending] = useState(false)
  const [viewLink, setViewLink] = useState<ShortLink | null>(null)

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setDebouncedSearch(search)
      setPage(0)
    }, 300)
    return () => window.clearTimeout(timer)
  }, [search])

  const load = async () => {
    setLoading(true)
    setLoadError('')
    try {
      const response = await linksApi.getLinks({
        page,
        pageSize: DEFAULT_PAGE_SIZE,
        search: debouncedSearch,
        status: statusFilter,
        sort,
      })
      setResult(response)
    } catch (caught) {
      setLoadError(toUserMessage(caught, 'Unable to load your links.'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void load()
  }, [page, debouncedSearch, statusFilter, sort])

  const changeFilter = (value: LinkDisplayStatus | 'ALL') => {
    setStatusFilter(value)
    setPage(0)
  }

  const changeSort = (value: SortOption) => {
    setSort(value)
    setPage(0)
  }

  const copy = async (url: string) => {
    try {
      await copyToClipboard(url)
      success('Short URL copied.')
    } catch {
      error('Could not copy the short URL.')
    }
  }

  const deleteLink = async () => {
    if (!deleteTarget) return
    setMutationPending(true)
    try {
      await linksApi.deleteLink(deleteTarget.id)
      success('Link deleted.')
      setDeleteTarget(null)
      void load()
    } catch (caught) {
      error(toUserMessage(caught, 'Could not delete this link.'))
    } finally {
      setMutationPending(false)
    }
  }

  const created = () => {
    setPage(0)
    void load()
  }

  const rows = result?.items ?? []

  return (
    <div className="space-y-6">
      <section className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-sm font-semibold text-signal">Link management</p>
          <h1 className="mt-1 text-3xl font-bold tracking-tight text-ink">All links</h1>
          <p className="mt-2 text-sm text-muted">
            Create, manage, and monitor every short link you own.
          </p>
        </div>
        <Button onClick={() => setCreateOpen(true)}>
          <Plus className="h-4 w-4" />
          New link
        </Button>
      </section>

      {loadError ? (
        <InlineError message={loadError} onRetry={() => void load()} />
      ) : loading && !result ? (
        <div className="h-80 animate-pulse rounded-xl bg-slate-200" />
      ) : rows.length === 0 && !loading ? (
        <EmptyState
          icon={<Link className="h-5 w-5" />}
          title={search || statusFilter !== 'ALL' ? 'No matching links' : 'No links yet'}
          description={
            search || statusFilter !== 'ALL'
              ? 'Try a different search or clear your filters.'
              : 'Create your first short link to start tracking clicks.'
          }
          action={
            !search && statusFilter === 'ALL' ? (
              <Button onClick={() => setCreateOpen(true)}>Create your first link</Button>
            ) : (
              <Button
                variant="secondary"
                onClick={() => {
                  setSearch('')
                  changeFilter('ALL')
                }}
              >
                Clear filters
              </Button>
            )
          }
        />
      ) : (
        <>
          <div className="flex flex-col gap-3 rounded-xl border border-line bg-white p-3 sm:flex-row">
            <label className="relative min-w-0 flex-1">
              <span className="sr-only">Search links</span>
              <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted" />
              <input
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Search short code or destination"
                className="min-h-10 w-full rounded-lg border border-line pl-9 pr-3 text-sm outline-none focus:border-signal"
              />
            </label>
            <select
              value={statusFilter}
              onChange={(e) => changeFilter(e.target.value as LinkDisplayStatus | 'ALL')}
              className="min-h-10 rounded-lg border border-line bg-white px-3 text-sm text-ink"
            >
              {STATUS_FILTERS.map((item) => (
                <option key={item.value} value={item.value}>
                  {item.label}
                </option>
              ))}
            </select>
            <select
              value={sort}
              onChange={(e) => changeSort(e.target.value as SortOption)}
              className="min-h-10 rounded-lg border border-line bg-white px-3 text-sm text-ink"
            >
              <option value="newest">Newest</option>
              <option value="oldest">Oldest</option>
              <option value="most-clicks">Most clicks</option>
              <option value="least-clicks">Least clicks</option>
            </select>
          </div>

          <div className="hidden overflow-hidden rounded-xl border border-line bg-white lg:block">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50 text-xs uppercase tracking-wide text-muted">
                <tr>
                  <th className="px-5 py-3 font-semibold">Link</th>
                  <th className="px-4 py-3 font-semibold">Status</th>
                  <th className="px-4 py-3 font-semibold">Clicks</th>
                  <th className="px-4 py-3 font-semibold">Created</th>
                  <th className="px-5 py-3 text-right font-semibold">Actions</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((link) => (
                  <tr key={link.id} className="border-t border-line">
                    <td className="max-w-md px-5 py-4">
                      <button
                        type="button"
                        onClick={() => setViewLink(link)}
                        className="font-mono text-sm font-semibold text-signal hover:underline text-left"
                      >
                        {link.shortUrl}
                      </button>
                      <p className="mt-1 truncate text-xs text-muted" title={link.originalUrl}>
                        {truncateUrl(link.originalUrl)}
                      </p>
                    </td>
                    <td className="px-4 py-4">
                      <LinkStatusBadge status={getLinkDisplayStatus(link)} />
                    </td>
                    <td className="px-4 py-4 font-medium text-ink">
                      {formatNumber(link.clicks)}
                    </td>
                    <td className="px-4 py-4 text-muted">{formatDate(link.createdAt)}</td>
                    <td className="px-5 py-4">
                      <Actions
                        link={link}
                        onCopy={copy}
                        onEdit={() => setEditTarget(link)}
                        onDelete={() => setDeleteTarget(link)}
                        pending={mutationPending}
                      />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="grid gap-3 lg:hidden">
            {rows.map((link) => (
              <article
                key={link.id}
                className="rounded-xl border border-line bg-white p-4"
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <p className="truncate font-mono text-sm font-semibold text-signal">
                      {link.shortUrl}
                    </p>
                    <p className="mt-1 break-all text-xs text-muted">{link.originalUrl}</p>
                  </div>
                  <LinkStatusBadge status={getLinkDisplayStatus(link)} />
                </div>
                <div className="mt-4 grid grid-cols-2 gap-3 border-t border-line pt-3 text-xs">
                  <div>
                    <p className="text-muted">Clicks</p>
                    <p className="mt-1 font-semibold text-ink">
                      {formatNumber(link.clicks)}
                    </p>
                  </div>
                  <div>
                    <p className="text-muted">Created</p>
                    <p className="mt-1 font-semibold text-ink">
                      {formatDate(link.createdAt)}
                    </p>
                  </div>
                </div>
                <div className="mt-4">
                  <Actions
                    link={link}
                    onCopy={copy}
                    onEdit={() => setEditTarget(link)}
                    onDelete={() => setDeleteTarget(link)}
                    pending={mutationPending}
                  />
                </div>
              </article>
            ))}
          </div>

          {result && result.totalPages > 1 && (
            <div className="flex items-center justify-between text-sm">
              <p className="text-muted">
                Page {result.page + 1} of {result.totalPages}
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
                  disabled={page >= result.totalPages - 1}
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

      <CreateLinkDialog
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        onCreated={() => void created()}
      />

      <EditLinkDialog
        link={editTarget}
        onClose={() => setEditTarget(null)}
        onUpdated={() => void load()}
      />

      <DeleteDialog
        target={deleteTarget}
        pending={mutationPending}
        onConfirm={deleteLink}
        onClose={() => !mutationPending && setDeleteTarget(null)}
      />

      <ViewLinkDialog
        link={viewLink}
        onClose={() => setViewLink(null)}
      />
    </div>
  )
}

function Actions({
  link,
  onCopy,
  onEdit,
  onDelete,
  pending,
}: {
  link: ShortLink
  onCopy: (url: string) => void
  onEdit: () => void
  onDelete: () => void
  pending: boolean
}) {
  return (
    <div className="flex items-center justify-end gap-1">
      <a
        href={link.originalUrl}
        target="_blank"
        rel="noreferrer"
        aria-label="Open destination URL"
        className="rounded-md p-2 text-muted hover:bg-slate-100 hover:text-ink"
      >
        <ExternalLink className="h-4 w-4" />
      </a>
      <button
        type="button"
        onClick={() => void onCopy(link.shortUrl)}
        aria-label="Copy short URL"
        className="rounded-md p-2 text-muted hover:bg-slate-100 hover:text-ink"
      >
        <Copy className="h-4 w-4" />
      </button>
      <button
        type="button"
        onClick={onEdit}
        aria-label="Edit link"
        disabled={pending}
        className="rounded-md p-2 text-muted hover:bg-slate-100 hover:text-ink disabled:opacity-55"
      >
        <Pencil className="h-4 w-4" />
      </button>
      <button
        type="button"
        onClick={onDelete}
        aria-label="Delete link"
        disabled={pending}
        className="rounded-md p-2 text-muted hover:bg-red-50 hover:text-danger disabled:opacity-55"
      >
        <Trash2 className="h-4 w-4" />
      </button>
    </div>
  )
}

function DeleteDialog({
  target,
  pending,
  onConfirm,
  onClose,
}: {
  target: ShortLink | null
  pending: boolean
  onConfirm: () => Promise<void>
  onClose: () => void
}) {
  return (
    <Dialog
      open={Boolean(target)}
      onClose={onClose}
      title="Delete this link?"
      description={
        target
          ? `${target.shortUrl} will stop redirecting and be removed from your links. This cannot be undone.`
          : undefined
      }
    >
      <div className="flex justify-end gap-3">
        <Button variant="secondary" onClick={onClose} disabled={pending}>
          Cancel
        </Button>
        <Button variant="danger" loading={pending} onClick={() => void onConfirm()}>
          {pending ? 'Deleting…' : 'Delete link'}
        </Button>
      </div>
    </Dialog>
  )
}

function ViewLinkDialog({
  link,
  onClose,
}: {
  link: ShortLink | null
  onClose: () => void
}) {
  if (!link) return null
  const status = getLinkDisplayStatus(link)
  return (
    <Dialog
      open={Boolean(link)}
      onClose={onClose}
      title="Link details"
    >
      <div className="space-y-4">
        <div>
          <p className="text-xs font-semibold uppercase tracking-wide text-muted">
            Short URL
          </p>
          <p className="mt-1 font-mono text-sm text-signal">{link.shortUrl}</p>
        </div>
        <div>
          <p className="text-xs font-semibold uppercase tracking-wide text-muted">
            Destination
          </p>
          <p className="mt-1 break-all text-sm text-ink">{link.originalUrl}</p>
        </div>
        <div className="grid grid-cols-3 gap-4">
          <div>
            <p className="text-xs font-semibold uppercase tracking-wide text-muted">
              Status
            </p>
            <div className="mt-1">
              <LinkStatusBadge status={status} />
            </div>
          </div>
          <div>
            <p className="text-xs font-semibold uppercase tracking-wide text-muted">
              Clicks
            </p>
            <p className="mt-1 font-semibold text-ink">
              {formatNumber(link.clicks)}
            </p>
          </div>
          <div>
            <p className="text-xs font-semibold uppercase tracking-wide text-muted">
              Code
            </p>
            <p className="mt-1 font-mono text-sm text-ink">{link.shortCode}</p>
          </div>
        </div>
        <div className="grid grid-cols-2 gap-4">
          <div>
            <p className="text-xs font-semibold uppercase tracking-wide text-muted">
              Created
            </p>
            <p className="mt-1 text-sm text-ink">{formatDate(link.createdAt)}</p>
          </div>
          <div>
            <p className="text-xs font-semibold uppercase tracking-wide text-muted">
              Updated
            </p>
            <p className="mt-1 text-sm text-ink">{formatDate(link.updatedAt)}</p>
          </div>
        </div>
        {link.expiresAt && (
          <div>
            <p className="text-xs font-semibold uppercase tracking-wide text-muted">
              Expires at
            </p>
            <p className="mt-1 text-sm text-ink">{formatDate(link.expiresAt)}</p>
          </div>
        )}
      </div>
      <div className="mt-6 flex justify-end gap-3">
        <Button variant="secondary" onClick={onClose}>
          Close
        </Button>
        <Button onClick={() => void copyToClipboard(link.shortUrl)}>
          <Copy className="h-4 w-4" />
          Copy link
        </Button>
      </div>
    </Dialog>
  )
}