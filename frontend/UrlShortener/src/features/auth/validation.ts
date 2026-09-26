export type FieldErrors = Record<string, string>

export function validateEmail(email: string) {
  if (!email.trim()) return 'Email is required.'
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return 'Enter a valid email address.'
  return ''
}

export function validateUsername(username: string) {
  if (!username.trim()) return 'Username is required.'
  if (!/^[a-zA-Z0-9_-]{3,20}$/.test(username.trim())) {
    return 'Use 3-20 characters: letters, numbers, underscore, or hyphen.'
  }
  return ''
}

export function validatePassword(password: string) {
  if (!password) return 'Password is required.'
  if (password.length < 8) return 'Use at least 8 characters.'
  return ''
}

export function validateLogin(values: { username: string; password: string }): FieldErrors {
  const errors: FieldErrors = {}
  const username = validateUsername(values.username)
  const password = validatePassword(values.password)
  if (username) errors.username = username
  if (password) errors.password = password
  return errors
}

export function validateRegister(values: { username: string; email: string; password: string; confirmPassword: string }): FieldErrors {
  const errors = validateLogin(values)
  const email = validateEmail(values.email)
  if (email) errors.email = email
  if (values.confirmPassword !== values.password) errors.confirmPassword = 'Passwords do not match.'
  return errors
}
