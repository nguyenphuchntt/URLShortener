import { describe, expect, it } from 'vitest'
import { getLinkDisplayStatus, type ShortLink } from './types'

const link: ShortLink = {
  id: 'link_1', shortCode: 'demo', shortUrl: 'https://sho.rt/demo', originalUrl: 'https://example.com', status: 'ACTIVE', clicks: 0, createdAt: '2026-08-01T00:00:00.000Z', updatedAt: '2026-08-01T00:00:00.000Z', expiresAt: '2026-08-10T00:00:00.000Z',
}

describe('getLinkDisplayStatus', () => {
  it('derives expired from an active link expiration date', () => {
    expect(getLinkDisplayStatus(link, new Date('2026-08-11T00:00:00.000Z'))).toBe('EXPIRED')
  })
  it('does not turn disabled links into expired links', () => {
    expect(getLinkDisplayStatus({ ...link, status: 'DISABLED' }, new Date('2026-08-11T00:00:00.000Z'))).toBe('DISABLED')
  })
})
