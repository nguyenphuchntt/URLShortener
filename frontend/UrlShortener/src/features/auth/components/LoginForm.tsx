import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { InlineError } from '@/components/feedback/InlineError'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { ROUTES } from '@/lib/constants'
import { toUserMessage } from '@/lib/utils'
import { useAuth } from '../AuthContext'
import { PasswordInput } from './PasswordInput'
import { validateLogin, type FieldErrors } from '../validation'

export function LoginForm() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [values, setValues] = useState({ username: '', password: '' })
  const [errors, setErrors] = useState<FieldErrors>({})
  const [formError, setFormError] = useState('')
  const [pending, setPending] = useState(false)

  const update = (key: keyof typeof values, value: string) => {
    setValues((v) => ({ ...v, [key]: value }))
    setErrors((e) => ({ ...e, [key]: '' }))
  }

  const submit = async (event: React.FormEvent) => {
    event.preventDefault()
    const next = validateLogin(values)
    setErrors(next)
    if (Object.keys(next).length) return
    setPending(true)
    setFormError('')
    try {
      await login(values)
      const state = location.state as { from?: { pathname?: string } } | null
      navigate(state?.from?.pathname ?? ROUTES.dashboard, { replace: true })
    } catch (error) {
      setFormError(toUserMessage(error, 'We could not sign you in. Please try again.'))
    } finally {
      setPending(false)
    }
  }

  return (
    <form onSubmit={submit} noValidate>
      <header className="mb-7">
        <p className="text-sm font-semibold text-signal">Welcome back</p>
        <h1 className="mt-1 text-2xl font-bold tracking-tight text-ink">Sign in to your workspace</h1>
      </header>
      {formError && (
        <div className="mb-4">
          <InlineError message={formError} />
        </div>
      )}
      <div className="space-y-4">
        <label className="block text-sm font-medium text-ink">
          Username
          <Input
            className="mt-1.5"
            type="text"
            autoComplete="username"
            value={values.username}
            onChange={(e) => update('username', e.target.value)}
            onBlur={() => setErrors((e) => ({ ...e, username: validateLogin(values).username ?? '' }))}
            error={errors.username}
          />
          {errors.username && <span className="mt-1 block text-xs text-danger">{errors.username}</span>}
        </label>
        <label className="block text-sm font-medium text-ink">
          Password
          <PasswordInput
            className="mt-1.5"
            autoComplete="current-password"
            value={values.password}
            onChange={(e) => update('password', e.target.value)}
            error={errors.password}
          />
          {errors.password && <span className="mt-1 block text-xs text-danger">{errors.password}</span>}
        </label>
      </div>
      <Button className="mt-6 w-full" type="submit" loading={pending}>
        {pending ? 'Signing in…' : 'Sign in'}
      </Button>
      <p className="mt-6 text-center text-sm text-muted">
        New to URL Shortener?{' '}
        <Link className="font-semibold text-signal hover:underline" to={ROUTES.register}>
          Create an account
        </Link>
      </p>
    </form>
  )
}
