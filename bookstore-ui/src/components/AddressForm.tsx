import { useState, type FormEvent } from 'react'
import { ApiError } from '../api'
import type { AddressInput } from '../types'

const empty: AddressInput = { label: '', recipientName: '', line1: '', line2: '', city: '', state: '', postalCode: '', country: 'India', phone: '' }

export default function AddressForm({ onSave, onCancel }: { onSave: (a: AddressInput) => Promise<void>; onCancel?: () => void }) {
  const [a, setA] = useState<AddressInput>(empty)
  const [busy, setBusy] = useState(false)
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [formError, setFormError] = useState('')
  const set = (k: keyof AddressInput) => (e: { target: { value: string } }) => setA(v => ({ ...v, [k]: e.target.value }))

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true); setErrors({}); setFormError('')
    // Optional fields must be omitted, not sent as '', or the backend's pattern checks reject them.
    const body: AddressInput = { ...a, phone: a.phone || undefined, line2: a.line2 || undefined, state: a.state || undefined, label: a.label || undefined }
    try {
      await onSave(body)
      setA(empty)
    } catch (err) {
      if (err instanceof ApiError && err.fieldErrors) setErrors(err.fieldErrors)
      else setFormError(err instanceof Error ? err.message : 'Could not save address')
    } finally {
      setBusy(false)
    }
  }

  const f = (k: keyof AddressInput, label: string, extra: { required?: boolean; span?: boolean } = {}) => (
    <label className={`field ${extra.span ? 'span-2' : ''}`}>{label}
      <input required={extra.required} value={(a[k] as string) ?? ''} onChange={set(k)} />
      {errors[k] && <span className="bad small">{errors[k]}</span>}
    </label>
  )

  return (
    <form onSubmit={submit} className="grid-2 address-form">
      {f('recipientName', 'Recipient name', { required: true, span: true })}
      {f('line1', 'Address line 1', { required: true, span: true })}
      {f('line2', 'Address line 2', { span: true })}
      {f('city', 'City', { required: true })}
      {f('state', 'State')}
      {f('postalCode', 'Postal code', { required: true })}
      {f('country', 'Country', { required: true })}
      {f('phone', 'Phone')}
      {f('label', 'Label (Home, Office…)')}
      {formError && <div className="alert alert-error span-2">{formError}</div>}
      <div className="span-2 row-gap">
        <button className="btn btn-primary" disabled={busy}>{busy ? 'Saving…' : 'Save address'}</button>
        {onCancel && <button type="button" className="btn" onClick={onCancel}>Cancel</button>}
      </div>
    </form>
  )
}
