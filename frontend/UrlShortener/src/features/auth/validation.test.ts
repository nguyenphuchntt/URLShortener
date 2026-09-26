import { describe, expect, it } from 'vitest'
import { validateLogin, validateRegister } from './validation'

describe('registration validation', () => {
  it('requires matching passwords and a valid email', () => {
    expect(
      validateRegister({
        username: 'A',
        email: 'bad-address',
        password: 'password123',
        confirmPassword: 'different',
      }),
    ).toMatchObject({
      username: 'Use 3-20 characters: letters, numbers, underscore, or hyphen.',
      email: 'Enter a valid email address.',
      confirmPassword: 'Passwords do not match.',
    })
  })

  it('accepts valid registration values', () => {
    expect(
      validateRegister({
        username: 'username',
        email: 'john@example.com',
        password: 'password123',
        confirmPassword: 'password123',
      }),
    ).toEqual({})
  })
})

describe('login validation', () => {
  it('requires a valid username and password', () => {
    expect(validateLogin({ username: '', password: '' })).toMatchObject({
      username: 'Username is required.',
      password: 'Password is required.',
    })
  })

  it('accepts valid login values', () => {
    expect(validateLogin({ username: 'username', password: 'password123' })).toEqual({})
  })
})
