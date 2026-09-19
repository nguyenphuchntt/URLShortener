import type {
  LinksApi,
  GetLinksParams,
  CreateLinkRequest,
  UpdateLinkStatusRequest,
  ShortLink,
  BackendLinkStatus,
} from '@/features/links/types'
import { DEFAULT_PAGE_SIZE, SHORT_DOMAIN } from '@/lib/constants'
import { http } from './client'

interface BackendShortUrl {
  shortCode: string
  originUrl: string
  status: BackendLinkStatus
  createdAt: string
  updatedAt: string
  expiresAt: string | null
}

interface BackendCreateResponse {
  createdAt: string
  originUrl: string
  shortCode: string
  expiresAt: string | null
}

interface SpringPage<T> {
  content: T[]
  number: number
  size: number
  totalElements: number
  totalPages: number
}

function toShortLink(url: BackendShortUrl | BackendCreateResponse): ShortLink {
  return {
    id: url.shortCode,
    shortCode: url.shortCode,
    shortUrl: `${SHORT_DOMAIN}/${url.shortCode}`,
    originalUrl: url.originUrl,
    status: 'status' in url ? url.status : 'ACTIVE',
    clicks: 0,
    createdAt: url.createdAt,
    updatedAt: 'updatedAt' in url ? url.updatedAt : url.createdAt,
    expiresAt: url.expiresAt ?? null,
  }
}

export const httpLinksApi: LinksApi = {
  async getLinks(params?: GetLinksParams) {
    const { page = 0, pageSize = DEFAULT_PAGE_SIZE, search = '', sort = 'newest' } = params ?? {}
    const query = new URLSearchParams({
      page: String(page),
      size: String(pageSize),
      sort: mapSort(sort),
    })
    if (search) query.set('search', search)

    const result = await http.get<SpringPage<BackendShortUrl>>(`/api/v1/urls/me?${query.toString()}`)
    return {
      items: result.content.map(toShortLink),
      page: result.number,
      pageSize: result.size,
      totalItems: result.totalElements,
      totalPages: result.totalPages,
    }
  },

  async getLink(id: string) {
    const result = await http.get<SpringPage<BackendShortUrl>>(
      `/api/v1/urls/me?page=0&size=1&search=${encodeURIComponent(id)}`,
    )
    const match = result.content.find((item) => item.shortCode === id)
    if (!match) {
      const error = new Error('The requested link could not be found.') as Error & { code: string; status: number }
      error.code = 'LINK_NOT_FOUND'
      error.status = 404
      throw error
    }
    return toShortLink(match)
  },

  async createLink(request: CreateLinkRequest) {
    const response = await http.post<BackendCreateResponse>('/api/v1/urls', {
      originUrl: request.originalUrl,
      customShortCode: request.customCode || undefined,
      expiresAt: request.expiresAt || undefined,
    })
    return toShortLink(response)
  },

  async updateStatus(id: string, request: UpdateLinkStatusRequest) {
    const response = await http.patch<BackendShortUrl>('/api/v1/urls', {
      shortCode: id,
      status: request.status,
    })
    return toShortLink(response)
  },
}

function mapSort(sort: string): string {
  switch (sort) {
    case 'oldest':
      return 'createdAt,asc'
    case 'most-clicks':
      return 'createdAt,desc'
    case 'least-clicks':
      return 'createdAt,asc'
    default:
      return 'createdAt,desc'
  }
}
