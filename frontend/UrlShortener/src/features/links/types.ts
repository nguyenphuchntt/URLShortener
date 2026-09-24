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

/**
 * Mirrors the backend PATCH /api/v1/urls body. `id` identifies the link,
 * `newCode` renames it, `expiresAt` reschedules expiry and `status` toggles it.
 * The destination URL cannot be changed by the backend — it is only echoed back
 * so the service can assert it still matches.
 */
export interface UpdateLinkRequest {
  newCode?: string
  status?: Extract<BackendLinkStatus, 'ACTIVE' | 'DISABLED'>
  expiresAt?: string | null
}

export interface LinksApi {
  getLinks(params?: GetLinksParams): Promise<PagedResponse<ShortLink>>
  getLink(id: string): Promise<ShortLink>
  createLink(request: CreateLinkRequest): Promise<ShortLink>
  updateStatus(id: string, request: UpdateLinkStatusRequest): Promise<ShortLink>
  updateLink(id: string, request: UpdateLinkRequest): Promise<ShortLink>
  deleteLink(id: string): Promise<void>
}

export function getLinkDisplayStatus(link: ShortLink, now = new Date()): LinkDisplayStatus {
  if (link.status === 'DELETED' || link.status === 'DISABLED') return link.status
  if (link.expiresAt && new Date(link.expiresAt).getTime() <= now.getTime()) return 'EXPIRED'
  return 'ACTIVE'
}
