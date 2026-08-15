import { describe, expect, it } from 'vitest'
import { validateRegister } from './validation'

describe('registration validation', () => {
  it('requires matching passwords and a valid email', () => {
    expect(validateRegister({ username: 'A', email: 'bad-address', password: 'password123', confirmPassword: 'different' })).toMatchObject({ username: 'Use at least 2 characters.', email: 'Enter a valid email address.', confirmPassword: 'Passwords do not match.' })
  })
})
