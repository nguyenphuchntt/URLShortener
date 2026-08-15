import type { ApiError } from '@/types/api'

export class MockApiError extends Error implements ApiError {
  code: string
  status?: number
  fieldErrors?: Record<string, string>

  constructor(code: string, message: string, status = 400, fieldErrors?: Record<string, string>) {
    super(message)
    this.name = 'MockApiError'
    this.code = code
    this.status = status
    this.fieldErrors = fieldErrors
  }
}
