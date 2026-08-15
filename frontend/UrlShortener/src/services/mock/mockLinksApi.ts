import type { LinksApi, GetLinksParams, CreateLinkRequest, UpdateLinkStatusRequest, ShortLink } from '@/features/links/types'
import { getLinkDisplayStatus } from '@/features/links/types'
import { SHORT_DOMAIN, DEFAULT_PAGE_SIZE } from '@/lib/constants'
import { MockApiError } from './mockError'
import { mockStore } from './mockStore'
import { mockDelay } from './mockDelay'

function resolveShortCode(customCode?: string): string {
  if (customCode) return customCode.trim().toLowerCase()
  const alphabet = 'abcdefghijklmnopqrstuvwxyz0123456789'
  let code = ''
  for (let i = 0; i < 6; i += 1) code += alphabet[Math.floor(Math.random() * alphabet.length)]
  return code
}

export const mockLinksApi: LinksApi = {
  async getLinks(params?: GetLinksParams) {
    await mockDelay()
    const { page = 0, pageSize = DEFAULT_PAGE_SIZE, search = '', status = 'ALL', sort = 'newest' } = params ?? {}
    const now = new Date()
    let items = mockStore.getLinks().map((link) => ({ ...link }))

    if (search) {
      const query = search.toLowerCase()
      items = items.filter((link) => link.shortCode.toLowerCase().includes(query) || link.originalUrl.toLowerCase().includes(query))
    }

    if (status && status !== 'ALL') {
      items = items.filter((link) => getLinkDisplayStatus(link, now) === status)
    }

    items.sort((a, b) => {
      if (sort === 'oldest') return new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
      if (sort === 'most-clicks') return b.clicks - a.clicks
      if (sort === 'least-clicks') return a.clicks - b.clicks
      return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    })

    const totalItems = items.length
    const totalPages = Math.max(1, Math.ceil(totalItems / pageSize))
    const start = page * pageSize
    const paged = items.slice(start, start + pageSize)

    return { items: paged, page: Math.min(page, totalPages - 1), pageSize, totalItems, totalPages }
  },

  async getLink(id: string) {
    await mockDelay()
    const link = mockStore.findLink(id)
    if (!link) throw new MockApiError('LINK_NOT_FOUND', 'The requested link could not be found.', 404)
    return { ...link }
  },

  async createLink(request: CreateLinkRequest) {
    await mockDelay()
    const originalUrl = request.originalUrl.trim()
    if (!originalUrl) {
      throw new MockApiError('INVALID_URL', 'Enter a destination URL.', 400, { originalUrl: 'Destination URL is required.' })
    }
    try {
      const parsed = new URL(originalUrl)
      if (parsed.protocol !== 'http:' && parsed.protocol !== 'https:') throw new Error('bad protocol')
    } catch {
      throw new MockApiError('INVALID_URL', 'Enter a full URL beginning with http:// or https://.', 400, { originalUrl: 'Enter a full URL beginning with http:// or https://.' })
    }

    const shortCode = resolveShortCode(request.customCode)
    if (shortCode.length > 16) {
      throw new MockApiError('INVALID_CUSTOM_CODE', 'Custom alias must be 16 characters or fewer.', 400, { customCode: 'Alias is too long.' })
    }
    if (request.customCode && mockStore.findLinkByCode(shortCode)) {
      throw new MockApiError('SHORT_CODE_ALREADY_EXISTS', 'The requested short code is already in use.', 409, { customCode: 'This alias is already taken.' })
    }

    const now = new Date().toISOString()
    const link: ShortLink = {
      id: `link_${String(Date.now()).slice(-6)}${Math.random().toString(16).slice(2, 4)}`,
      shortCode,
      shortUrl: `${SHORT_DOMAIN}/${shortCode}`,
      originalUrl,
      status: 'ACTIVE',
      clicks: 0,
      createdAt: now,
      updatedAt: now,
      expiresAt: request.expiresAt ?? null,
    }

    return mockStore.addLink({ ...link })
  },

  async updateStatus(id: string, request: UpdateLinkStatusRequest) {
    await mockDelay()
    const updated = mockStore.updateLink(id, (link) => ({
      ...link,
      status: request.status,
      updatedAt: new Date().toISOString(),
    }))
    if (!updated) throw new MockApiError('LINK_NOT_FOUND', 'The requested link could not be found.', 404)
    return { ...updated }
  },
}
