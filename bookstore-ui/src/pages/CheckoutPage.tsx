import { useEffect, useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { api, dateFmt, errMsg, money } from '../api'
import { useAsync } from '../hooks'
import { useStore } from '../store'
import AddressForm from '../components/AddressForm'
import { usePaymentForm } from '../components/PaymentFields'
import { ErrorBox, Loading } from '../components/Status'

export default function CheckoutPage() {
  const { user, refreshCart, refreshUser, notify } = useStore()
  const nav = useNavigate()
  const [points, setPoints] = useState(0)
  const [addressId, setAddressId] = useState<number | null>(null)
  const [addingAddress, setAddingAddress] = useState(false)
  const [busy, setBusy] = useState(false)
  const [done, setDone] = useState(false) // stops the empty-cart redirect after the cart is cleared by checkout

  const addresses = useAsync(() => api.addresses(), [])
  const preview = useAsync(() => api.preview(points), [points])
  const pay = usePaymentForm(preview.data?.paymentMethods ?? [
    { method: 'CREDIT_CARD', label: 'Credit card', requires: 'card' },
    { method: 'DEBIT_CARD', label: 'Debit card', requires: 'card' },
    { method: 'UPI', label: 'UPI', requires: 'upiId' },
    { method: 'NET_BANKING', label: 'Net banking', requires: 'bankCode' },
    { method: 'CASH_ON_DELIVERY', label: 'Cash on delivery', requires: 'nothing' },
  ])

  // Default to the user's default address once loaded.
  useEffect(() => {
    if (addressId == null && addresses.data && addresses.data.length > 0) {
      setAddressId((addresses.data.find(a => a.isDefault) ?? addresses.data[0]).id)
    }
  }, [addresses.data, addressId])

  // A points value rejected by the server (e.g. above the cap) is clamped to what is allowed.
  useEffect(() => {
    if (preview.error && 'code' in preview.error && ['POINTS_LIMIT_EXCEEDED', 'INSUFFICIENT_POINTS'].includes(String((preview.error as { code: string }).code))) {
      setPoints(0)
    }
  }, [preview.error])

  const p = preview.data
  if (!done && p && p.cart.items.length === 0) return <Navigate to="/cart" replace />

  async function placeAndPay() {
    if (addressId == null) { notify('Please choose a delivery address', 'error'); return }
    const req = pay.build()
    if (typeof req === 'string') { notify(req, 'error'); return }
    setBusy(true)
    let orderId: number | null = null
    try {
      const order = await api.placeOrder(addressId, points)
      orderId = order.id
      setDone(true)
      const result = await api.pay(order.id, req)
      await Promise.all([refreshCart(), refreshUser()])
      nav(`/orders/${order.id}`, { state: { message: result.message, failed: result.payment.status === 'FAILED' } })
    } catch (e) {
      notify(errMsg(e), 'error')
      if (orderId != null) { await refreshCart(); nav(`/orders/${orderId}`) }
    } finally {
      setBusy(false)
    }
  }

  return (
    <div>
      <h1>Checkout</h1>
      {preview.error && !preview.data && <ErrorBox error={preview.error} retry={preview.reload} />}
      {preview.loading && !p && <Loading />}
      {p && (
        <div className="cart-layout">
          <div className="stack">
            <section className="card pad">
              <h3>1 · Delivery address</h3>
              {addresses.data?.map(a => (
                <label key={a.id} className={`addr ${addressId === a.id ? 'addr-on' : ''}`}>
                  <input type="radio" name="addr" checked={addressId === a.id} onChange={() => setAddressId(a.id)} />
                  <span>
                    <b>{a.recipientName}</b>{a.label ? ` · ${a.label}` : ''}{a.isDefault && <span className="pill pill-confirmed">Default</span>}<br />
                    {a.line1}{a.line2 ? `, ${a.line2}` : ''}, {a.city}{a.state ? `, ${a.state}` : ''} {a.postalCode}, {a.country}
                  </span>
                </label>
              ))}
              {addresses.data?.length === 0 && !addingAddress && <p className="muted">You have no saved addresses yet.</p>}
              {addingAddress || addresses.data?.length === 0 ? (
                <AddressForm
                  onCancel={addresses.data && addresses.data.length > 0 ? () => setAddingAddress(false) : undefined}
                  onSave={async a => {
                    const saved = await api.addAddress(a)
                    addresses.reload()
                    setAddressId(saved.id)
                    setAddingAddress(false)
                  }} />
              ) : (
                <button className="btn" onClick={() => setAddingAddress(true)}>+ Add a new address</button>
              )}
            </section>

            <section className="card pad">
              <h3>2 · Gift points</h3>
              <p className="muted small">You have <b>{p.giftPointBalance}</b> points (1 point = {money(1)}). You can use up to <b>{p.maxRedeemablePoints}</b> on this order.</p>
              <div className="row-gap">
                <input type="number" min={0} max={p.maxRedeemablePoints} value={points}
                  onChange={e => setPoints(Math.max(0, Math.min(p.maxRedeemablePoints, Math.floor(Number(e.target.value)) || 0)))} />
                <button className="btn" onClick={() => setPoints(p.maxRedeemablePoints)} disabled={p.maxRedeemablePoints === 0}>Use maximum</button>
                <button className="btn btn-link" onClick={() => setPoints(0)}>Clear</button>
              </div>
            </section>

            <section className="card pad">
              <h3>3 · Payment</h3>
              {pay.fields}
            </section>
          </div>

          <aside className="card summary">
            <h3>Order summary</h3>
            {p.cart.items.map(i => (
              <div key={i.productId} className="row-between small"><span>{i.title} × {i.quantity}</span><span>{money(i.lineTotal)}</span></div>
            ))}
            <hr />
            <div className="row-between"><span>Subtotal</span><span>{money(p.subtotal)}</span></div>
            {p.discount > 0 && <div className="row-between ok"><span>Gift points ({p.pointsToRedeem})</span><span>− {money(p.discount)}</span></div>}
            <div className="row-between total"><span>Total</span><b>{money(p.total)}</b></div>
            {p.cart.estimatedDeliveryDate && <p className="small muted">Estimated delivery {dateFmt(p.cart.estimatedDeliveryDate)}</p>}
            <p className="small muted">You'll earn <b>{p.pointsToEarn}</b> gift points on this purchase.</p>
            <button className="btn btn-primary btn-block" disabled={busy || preview.loading || !p.cart.readyForCheckout} onClick={placeAndPay}>
              {busy ? 'Processing…' : `Pay ${money(p.total)}`}
            </button>
            {!p.cart.readyForCheckout && <p className="bad small">Some items are no longer available. <Link to="/cart">Review your cart</Link></p>}
            {user && <p className="muted small">Signed in as {user.email}</p>}
          </aside>
        </div>
      )}
    </div>
  )
}
