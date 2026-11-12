import { useState, type FormEvent } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { ApiError, api } from '../api'
import { useStore } from '../store'

export default function LoginPage() {
  const { user, signIn, notify } = useStore()
  const nav = useNavigate()
  const from = (useLocation().state as { from?: string } | null)?.from ?? '/'
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [email, setEmail] = useState('reader@bookstore.local')
  const [password, setPassword] = useState('Reader@123')
  const [fullName, setFullName] = useState('')
  const [phone, setPhone] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [fields, setFields] = useState<Record<string, string>>({})

  if (user) return <Navigate to={from} replace />

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true); setError(''); setFields({})
    try {
      const res = mode === 'login'
        ? await api.login(email.trim(), password)
        : await api.register({ email: email.trim(), password, fullName: fullName.trim(), phone: phone.trim() || undefined })
      signIn(res.accessToken, res.user)
      notify(mode === 'login' ? `Welcome back, ${res.user.fullName}!` : 'Account created. Welcome!')
      nav(from, { replace: true })
    } catch (err) {
      if (err instanceof ApiError && err.fieldErrors) setFields(err.fieldErrors)
      setError(err instanceof Error ? err.message : 'Sign in failed')
    } finally {
      setBusy(false)
    }
  }

  const switchMode = (m: 'login' | 'register') => {
    setMode(m); setError(''); setFields({})
    if (m === 'register') { setEmail(''); setPassword('') } else { setEmail('reader@bookstore.local'); setPassword('Reader@123') }
  }

  return (
    <div className="auth-card card">
      <h1>{mode === 'login' ? 'Sign in' : 'Create your account'}</h1>
      <form onSubmit={submit} className="stack">
        {mode === 'register' && (
          <label className="field">Full name
            <input required value={fullName} onChange={e => setFullName(e.target.value)} />
            {fields.fullName && <span className="bad small">{fields.fullName}</span>}
          </label>
        )}
        <label className="field">Email
          <input required type="email" autoComplete="email" value={email} onChange={e => setEmail(e.target.value)} />
          {fields.email && <span className="bad small">{fields.email}</span>}
        </label>
        <label className="field">Password
          <input required type="password" autoComplete={mode === 'login' ? 'current-password' : 'new-password'} value={password} onChange={e => setPassword(e.target.value)} />
          {fields.password && <span className="bad small">{fields.password}</span>}
        </label>
        {mode === 'register' && (
          <label className="field">Phone (optional)
            <input value={phone} onChange={e => setPhone(e.target.value)} />
            {fields.phone && <span className="bad small">{fields.phone}</span>}
          </label>
        )}
        {error && <div className="alert alert-error">{error}</div>}
        <button className="btn btn-primary" disabled={busy}>{busy ? 'Please wait…' : mode === 'login' ? 'Sign in' : 'Create account'}</button>
      </form>
      {mode === 'login' ? (
        <>
          <p className="muted small">Demo customer prefilled: reader@bookstore.local (200 gift points).</p>
          <p>New here? <button className="btn btn-link" onClick={() => switchMode('register')}>Create an account</button></p>
        </>
      ) : (
        <p>Already registered? <button className="btn btn-link" onClick={() => switchMode('login')}>Sign in</button></p>
      )}
    </div>
  )
}
