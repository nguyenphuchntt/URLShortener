import type { PagedResponse, SortOption } from '@/types/api'

export type BackendLinkStatus = 'ACTIVE' | 'DISABLED' | 'DELETED'
export type LinkDisplayStatus = BackendLinkStatus | 'EXPIRED'

export interface ShortLink {
  id: string
  shortCode: string
  shortUrl: string
  originalUrl: string
  status: BackendLinkStatus
  clicks: number
  createdAt: string
  updatedAt: string
  expiresAt: string | null
}

export interface GetLinksParams {
  page?: number
  pageSize?: number
  search?: string
  status?: LinkDisplayStatus | 'ALL'
  sort?: SortOption
}

export interface CreateLinkRequest {
  originalUrl: string
  customCode?: string
  expiresAt?: string | null
}

export interface UpdateLinkStatusRequest {
  status: Extract<BackendLinkStatus, 'ACTIVE' | 'DISABLED'>
}

export interface LinksApi {
  getLinks(params?: GetLinksParams): Promise<PagedResponse<ShortLink>>
  getLink(id: string): Promise<ShortLink>
  createLink(request: CreateLinkRequest): Promise<ShortLink>
  updateStatus(id: string, request: UpdateLinkStatusRequest): Promise<ShortLink>
}

export function getLinkDisplayStatus(link: ShortLink, now = new Date()): LinkDisplayStatus {
  if (link.status === 'DELETED' || link.status === 'DISABLED') return link.status
  if (link.expiresAt && new Date(link.expiresAt).getTime() <= now.getTime()) return 'EXPIRED'
  return 'ACTIVE'
}
