import { useState } from 'react'
import type { PaymentMethod, PaymentMethodOption, PaymentRequest } from '../types'

const BANKS = ['HDFC', 'ICICI', 'SBI', 'AXIS', 'KOTAK']

/** Collects the details for the chosen payment method and exposes them as a PaymentRequest. */
export function usePaymentForm(methods: PaymentMethodOption[]) {
  const [method, setMethod] = useState<PaymentMethod>('CREDIT_CARD')
  const [number, setNumber] = useState('')
  const [holder, setHolder] = useState('')
  const [expiry, setExpiry] = useState('') // MM/YY
  const [cvv, setCvv] = useState('')
  const [upi, setUpi] = useState('')
  const [bank, setBank] = useState(BANKS[0])

  const build = (): PaymentRequest | string => {
    if (method === 'CREDIT_CARD' || method === 'DEBIT_CARD') {
      const m = /^(\d{1,2})\s*\/\s*(\d{2}|\d{4})$/.exec(expiry.trim())
      if (!number.trim() || !holder.trim() || !m || !cvv.trim()) return 'Please fill in all card details (expiry as MM/YY).'
      const year = m[2].length === 2 ? 2000 + Number(m[2]) : Number(m[2])
      return { method, card: { number, holderName: holder.trim(), expiryMonth: Number(m[1]), expiryYear: year, cvv } }
    }
    if (method === 'UPI') return upi.trim() ? { method, upiId: upi.trim() } : 'Please enter your UPI id (like name@okbank).'
    if (method === 'NET_BANKING') return { method, bankCode: bank }
    return { method }
  }

  const fields = (
    <fieldset className="pay-fields">
      <legend>Payment method</legend>
      <div className="pay-methods">
        {methods.map(m => (
          <label key={m.method} className={`chip ${method === m.method ? 'chip-on' : ''}`}>
            <input type="radio" name="pay-method" checked={method === m.method} onChange={() => setMethod(m.method)} />
            {m.label}
          </label>
        ))}
      </div>

      {(method === 'CREDIT_CARD' || method === 'DEBIT_CARD') && (
        <div className="grid-2">
          <label className="field span-2">Card number
            <input inputMode="numeric" autoComplete="cc-number" placeholder="4111 1111 1111 1111" value={number} onChange={e => setNumber(e.target.value)} />
          </label>
          <label className="field span-2">Name on card
            <input autoComplete="cc-name" value={holder} onChange={e => setHolder(e.target.value)} />
          </label>
          <label className="field">Expiry (MM/YY)
            <input autoComplete="cc-exp" placeholder="12/30" value={expiry} onChange={e => setExpiry(e.target.value)} />
          </label>
          <label className="field">CVV
            <input inputMode="numeric" autoComplete="cc-csc" maxLength={4} type="password" value={cvv} onChange={e => setCvv(e.target.value)} />
          </label>
          <p className="muted small span-2">Demo cards: <code>4111 1111 1111 1111</code> is approved, <code>4000 0000 0000 0002</code> is declined. Use any future expiry.</p>
        </div>
      )}
      {method === 'UPI' && (
        <label className="field">UPI id
          <input placeholder="name@okbank" value={upi} onChange={e => setUpi(e.target.value)} />
          <span className="muted small">Demo: ids ending in <code>@fail</code> are declined.</span>
        </label>
      )}
      {method === 'NET_BANKING' && (
        <label className="field">Bank
          <select value={bank} onChange={e => setBank(e.target.value)}>
            {BANKS.map(b => <option key={b}>{b}</option>)}
          </select>
        </label>
      )}
      {method === 'CASH_ON_DELIVERY' && <p className="muted">Pay in cash when your books arrive.</p>}
    </fieldset>
  )

  return { fields, build, method }
}
