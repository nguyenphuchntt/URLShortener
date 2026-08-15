import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { InlineError } from '@/components/feedback/InlineError'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { ROUTES } from '@/lib/constants'
import { toUserMessage } from '@/lib/utils'
import { useAuth } from '../AuthContext'
import { PasswordInput } from './PasswordInput'
import { validateRegister, type FieldErrors } from '../validation'

export function RegisterForm() {
  const { register } = useAuth(); const navigate = useNavigate()
  const [values, setValues] = useState({ username: '', email: '', password: '', confirmPassword: '' }); const [errors, setErrors] = useState<FieldErrors>({}); const [formError, setFormError] = useState(''); const [pending, setPending] = useState(false)
  const update = (key: keyof typeof values, value: string) => { setValues((v) => ({ ...v, [key]: value })); setErrors((e) => ({ ...e, [key]: '' })) }
  const submit = async (event: React.FormEvent) => { event.preventDefault(); const next = validateRegister(values); setErrors(next); if (Object.keys(next).length) return; setPending(true); setFormError(''); try { await register({ username: values.username.trim(), email: values.email.trim(), password: values.password }); navigate(ROUTES.dashboard, { replace: true }) } catch (error) { const apiError = error as { fieldErrors?: FieldErrors }; setErrors(apiError.fieldErrors ?? {}); setFormError(toUserMessage(error, 'We could not create your account. Please try again.')) } finally { setPending(false) } }
  return <form onSubmit={submit} noValidate><header className="mb-7"><p className="text-sm font-semibold text-signal">Get started</p><h1 className="mt-1 text-2xl font-bold tracking-tight text-ink">Create your workspace</h1><p className="mt-2 text-sm text-muted">Start organizing every important link in one place.</p></header>{formError && <div className="mb-4"><InlineError message={formError} /></div>}<div className="space-y-4">{([['username', 'Display name', 'text'], ['email', 'Email', 'email']] as const).map(([key, label, type]) => <label key={key} className="block text-sm font-medium text-ink">{label}<Input className="mt-1.5" type={type} autoComplete={key === 'email' ? 'email' : 'name'} value={values[key]} onChange={(e) => update(key, e.target.value)} error={errors[key]} />{errors[key] && <span className="mt-1 block text-xs text-danger">{errors[key]}</span>}</label>)}<label className="block text-sm font-medium text-ink">Password<PasswordInput className="mt-1.5" autoComplete="new-password" value={values.password} onChange={(e) => update('password', e.target.value)} error={errors.password} />{errors.password && <span className="mt-1 block text-xs text-danger">{errors.password}</span>}</label><label className="block text-sm font-medium text-ink">Confirm password<PasswordInput className="mt-1.5" autoComplete="new-password" value={values.confirmPassword} onChange={(e) => update('confirmPassword', e.target.value)} error={errors.confirmPassword} />{errors.confirmPassword && <span className="mt-1 block text-xs text-danger">{errors.confirmPassword}</span>}</label></div><Button className="mt-6 w-full" type="submit" loading={pending}>{pending ? 'Creating account…' : 'Create account'}</Button><p className="mt-6 text-center text-sm text-muted">Already have an account? <Link className="font-semibold text-signal hover:underline" to={ROUTES.login}>Sign in</Link></p></form>
}
