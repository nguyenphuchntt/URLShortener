import { useEffect, useState } from 'react'
import { Save } from 'lucide-react'
import { Dialog } from '@/components/ui/Dialog'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { useApi } from '@/app/providers'
import { useToast } from '@/components/feedback/ToastProvider'
import { toUserMessage } from '@/lib/utils'
import type { FieldErrors } from '@/features/auth/validation'
import { SHORT_DOMAIN } from '@/lib/constants'
import type { BackendLinkStatus, ShortLink } from '../types'

/** `datetime-local` wants `YYYY-MM-DDTHH:mm` in the browser's zone, not an ISO instant. */
function toDateTimeLocal(iso: string | null): string {
  if (!iso) return ''
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return ''
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function validate(values: { code: string; expiresAt: string }): FieldErrors {
  const errors: FieldErrors = {}
  if (values.code && !/^[a-zA-Z0-9]{6,16}$/.test(values.code)) {
    errors.customCode = 'Use 6–16 letters or numbers.'
  }
  if (values.expiresAt && new Date(values.expiresAt).getTime() <= Date.now()) {
    errors.expiresAt = 'Choose a future date and time.'
  }
  return errors
}

export function EditLinkDialog({
  link,
  onClose,
  onUpdated,
}: {
  link: ShortLink | null
  onClose: () => void
  onUpdated: () => void
}) {
  const { links } = useApi()
  const { success, error } = useToast()
  const [code, setCode] = useState('')
  const [status, setStatus] = useState<Extract<BackendLinkStatus, 'ACTIVE' | 'DISABLED'>>('ACTIVE')
  const [expiresAt, setExpiresAt] = useState('')
  const [errors, setErrors] = useState<FieldErrors>({})
  const [pending, setPending] = useState(false)

  useEffect(() => {
    if (!link) return
    setCode(link.shortCode)
    setStatus(link.status === 'DISABLED' ? 'DISABLED' : 'ACTIVE')
    setExpiresAt(toDateTimeLocal(link.expiresAt))
    setErrors({})
    setPending(false)
  }, [link])

  const close = () => {
    if (!pending) onClose()
  }

  const submit = async (event: React.FormEvent) => {
    event.preventDefault()
    if (!link) return
    const next = validate({ code, expiresAt })
    setErrors(next)
    if (Object.keys(next).length) return

    const trimmedCode = code.trim()
    setPending(true)
    try {
      await links.updateLink(link.shortCode, {
        newCode: trimmedCode && trimmedCode !== link.shortCode ? trimmedCode : undefined,
        status,
        expiresAt: expiresAt ? new Date(expiresAt).toISOString() : null,
      })
      success('Link updated.')
      onUpdated()
      onClose()
    } catch (caught) {
      const apiError = caught as { fieldErrors?: FieldErrors }
      setErrors(apiError.fieldErrors ?? {})
      error(toUserMessage(caught, 'Could not update this link.'))
    } finally {
      setPending(false)
    }
  }

  return (
    <Dialog
      open={Boolean(link)}
      onClose={close}
      title="Edit link"
      description={link ? `Update settings for ${link.shortUrl}.` : undefined}
    >
      {link && (
        <form onSubmit={submit} noValidate className="space-y-4">
          <div>
            <p className="text-xs font-semibold uppercase tracking-wide text-muted">Destination</p>
            <p className="mt-1 break-all text-sm text-ink">{link.originalUrl}</p>
            <p className="mt-1 text-xs text-muted">
              The destination cannot be changed. Delete and recreate the link to point elsewhere.
            </p>
          </div>

          <label className="block text-sm font-medium text-ink">
            Short code
            <div className="mt-1.5 flex">
              <span className="flex min-h-11 items-center rounded-l-lg border border-r-0 border-line bg-slate-50 px-3 text-sm text-muted">
                {SHORT_DOMAIN.replace(/^https?:\/\//, '')}/
              </span>
              <Input
                className="rounded-l-none"
                value={code}
                onChange={(e) => {
                  setCode(e.target.value)
                  setErrors((state) => ({ ...state, customCode: '' }))
                }}
                error={errors.customCode}
              />
            </div>
            {errors.customCode && <span className="mt-1 block text-xs text-danger">{errors.customCode}</span>}
          </label>

          <label className="block text-sm font-medium text-ink">
            Status
            <select
              value={status}
              onChange={(e) => setStatus(e.target.value as 'ACTIVE' | 'DISABLED')}
              className="mt-1.5 min-h-11 w-full rounded-lg border border-line bg-white px-3 text-sm text-ink"
            >
              <option value="ACTIVE">Active</option>
              <option value="DISABLED">Disabled</option>
            </select>
          </label>

          <label className="block text-sm font-medium text-ink">
            Expiration <span className="font-normal text-muted">(optional)</span>
            <Input
              className="mt-1.5"
              type="datetime-local"
              value={expiresAt}
              onChange={(e) => {
                setExpiresAt(e.target.value)
                setErrors((state) => ({ ...state, expiresAt: '' }))
              }}
              error={errors.expiresAt}
            />
            {errors.expiresAt && <span className="mt-1 block text-xs text-danger">{errors.expiresAt}</span>}
          </label>

          <div className="flex justify-end gap-3 pt-2">
            <Button variant="secondary" type="button" onClick={close} disabled={pending}>
              Cancel
            </Button>
            <Button type="submit" loading={pending}>
              {pending ? 'Saving…' : (
                <>
                  <Save className="h-4 w-4" />
                  Save changes
                </>
              )}
            </Button>
          </div>
        </form>
      )}
    </Dialog>
  )
}
