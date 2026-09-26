import type { FieldErrors } from '@/features/auth/validation'

export function validateLink(values: { originalUrl: string; customCode: string; expiresAt: string }): FieldErrors {
  const errors: FieldErrors = {}
  try {
    const parsed = new URL(values.originalUrl.trim())
    if (!['http:', 'https:'].includes(parsed.protocol)) throw new Error('protocol')
  } catch { errors.originalUrl = 'Enter a full URL beginning with http:// or https://.' }
  if (values.customCode && !/^[a-z0-9-]{3,16}$/.test(values.customCode)) errors.customCode = 'Use 3–16 lowercase letters, numbers, or hyphens.'
  if (values.expiresAt && new Date(values.expiresAt).getTime() <= Date.now()) errors.expiresAt = 'Choose a future date and time.'
  return errors
}
