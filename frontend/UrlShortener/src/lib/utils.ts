import type { ApiError } from '@/types/api'

export function cn(...classes: Array<string | false | null | undefined>) {
  return classes.filter(Boolean).join(' ')
}

export function formatNumber(value: number) {
  return new Intl.NumberFormat('en-US').format(value)
}

export function formatDate(value: string | null) {
  if (!value) return 'Never'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'Unknown'
  return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric' }).format(date)
}

export function formatChartDate(value: string) {
  return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric' }).format(new Date(`${value}T12:00:00`))
}

export function truncateUrl(value: string, max = 54) {
  return value.length > max ? `${value.slice(0, max - 1)}…` : value
}

export function getUserLabel(username: string) {
  return username.trim().split(/[._-]/)[0] || username
}

export function toUserMessage(error: unknown, fallback = 'Something went wrong. Please try again.') {
  if (typeof error === 'object' && error !== null && 'message' in error) {
    const message = (error as ApiError).message
    if (typeof message === 'string' && message.length > 0) return message
  }
  return fallback
}

export async function copyToClipboard(value: string) {
  if (!navigator.clipboard) throw new Error('Clipboard unavailable')
  await navigator.clipboard.writeText(value)
}
