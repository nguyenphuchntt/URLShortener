export interface ApiError {
  code: string
  message: string
  status?: number
  fieldErrors?: Record<string, string>
}

export interface PagedResponse<T> {
  items: T[]
  page: number
  pageSize: number
  totalItems: number
  totalPages: number
}

export type SortOption = 'newest' | 'oldest' | 'most-clicks' | 'least-clicks'

export interface AsyncState<T> {
  data: T | null
  loading: boolean
  error: ApiError | null
}
