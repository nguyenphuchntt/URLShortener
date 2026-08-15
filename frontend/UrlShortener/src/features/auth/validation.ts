export type FieldErrors = Record<string, string>

export function validateEmail(email: string) {
  if (!email.trim()) return 'Email is required.'
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return 'Enter a valid email address.'
  return ''
}

export function validatePassword(password: string) {
  if (!password) return 'Password is required.'
  if (password.length < 8) return 'Use at least 8 characters.'
  return ''
}

export function validateLogin(values: { email: string; password: string }): FieldErrors {
  const errors: FieldErrors = {}
  const email = validateEmail(values.email); const password = validatePassword(values.password)
  if (email) errors.email = email
  if (password) errors.password = password
  return errors
}

export function validateRegister(values: { username: string; email: string; password: string; confirmPassword: string }): FieldErrors {
  const errors = validateLogin(values)
  if (!values.username.trim()) errors.username = 'Display name is required.'
  else if (values.username.trim().length < 2) errors.username = 'Use at least 2 characters.'
  if (values.confirmPassword !== values.password) errors.confirmPassword = 'Passwords do not match.'
  return errors
}
