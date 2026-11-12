import { useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { api, dateFmt, dateTimeFmt, errMsg, money } from '../api'
import { useAsync } from '../hooks'
import { useStore } from '../store'
import { usePaymentForm } from '../components/PaymentFields'
import { ErrorBox, Loading, StatusPill } from '../components/Status'
import type { Order } from '../types'

const METHODS = [
  { method: 'CREDIT_CARD', label: 'Credit card', requires: 'card' },
  { method: 'DEBIT_CARD', label: 'Debit card', requires: 'card' },
  { method: 'UPI', label: 'UPI', requires: 'upiId' },
  { method: 'NET_BANKING', label: 'Net banking', requires: 'bankCode' },
  { method: 'CASH_ON_DELIVERY', label: 'Cash on delivery', requires: 'nothing' },
] as const

function RetryPayment({ order, onPaid }: { order: Order; onPaid: (o: Order, message: string, failed: boolean) => void }) {
  const pay = usePaymentForm([...METHODS])
  const [busy, setBusy] = useState(false)
  const { notify } = useStore()
  async function submit() {
    const req = pay.build()
    if (typeof req === 'string') { notify(req, 'error'); return }
    setBusy(true)
    try {
      const r = await api.pay(order.id, req)
      onPaid(r.order, r.message, r.payment.status === 'FAILED')
    } catch (e) { notify(errMsg(e), 'error') } finally { setBusy(false) }
  }
  return (
    <section className="card pad">
      <h3>Complete your payment · {money(order.total)}</h3>
      {pay.fields}
      <button className="btn btn-primary" disabled={busy} onClick={submit}>{busy ? 'Processing…' : `Pay ${money(order.total)}`}</button>
    </section>
  )
}

export default function OrderPage() {
  const id = Number(useParams().id)
  const nav = useNavigate()
  const flash = useLocation().state as { message?: string; failed?: boolean } | null
  const { setCart, refreshUser, notify } = useStore()
  const { data: order, error, loading, reload, setData } = useAsync(() => api.order(id), [id])
  const [message, setMessage] = useState<{ text: string; failed: boolean } | null>(
    flash?.message ? { text: flash.message, failed: !!flash.failed } : null)
  const [busy, setBusy] = useState(false)

  if (loading && !order) return <Loading />
  if (error) return <ErrorBox error={error} retry={reload} />
  if (!order) return null

  async function cancel() {
    if (!window.confirm('Cancel this order? Your payment will be refunded and gift points returned.')) return
    setBusy(true)
    try {
      const o = await api.cancelOrder(id)
      setData(o); setMessage({ text: o.message, failed: false }); await refreshUser()
    } catch (e) { notify(errMsg(e), 'error') } finally { setBusy(false) }
  }

  async function buyAgain() {
    setBusy(true)
    try {
      const r = await api.buyAgain(id)
      setCart(r.cart)
      notify(r.skipped.length ? `Added ${r.added.length} item(s); skipped: ${r.skipped.join(', ')}` : 'Items added to your cart')
      nav('/cart')
    } catch (e) { notify(errMsg(e), 'error') } finally { setBusy(false) }
  }

  const a = order.shippingAddress
  return (
    <div>
      <p className="small"><Link to="/orders">← My orders</Link></p>
      {message && <div className={`alert ${message.failed ? 'alert-error' : 'alert-ok'}`}>{message.text}</div>}
      <div className="row-between">
        <h1>Order {order.orderNumber}</h1>
        <StatusPill status={order.status} />
      </div>
      <p className="muted">Placed {dateTimeFmt(order.placedAt)}{order.status !== 'CANCELLED' && ` · estimated delivery ${dateFmt(order.estimatedDeliveryDate)}`}</p>

      <div className="cart-layout">
        <div className="stack">
          <section className="card pad">
            <h3>Items</h3>
            {order.items.map(i => (
              <div key={i.productId} className="row-between">
                <Link to={`/products/${i.productId}`}>{i.title}</Link>
                <span>{i.quantity} × {money(i.unitPrice)} = <b>{money(i.lineTotal)}</b></span>
              </div>
            ))}
          </section>
          <section className="card pad">
            <h3>Delivering to</h3>
            <p>{a.recipientName}<br />{a.line1}{a.line2 ? `, ${a.line2}` : ''}<br />{a.city}{a.state ? `, ${a.state}` : ''} {a.postalCode}, {a.country}</p>
          </section>
          {order.payments.length > 0 && (
            <section className="card pad">
              <h3>Payments</h3>
              {order.payments.map(p => (
                <div key={p.id} className="row-between small">
                  <span>{p.method.replace(/_/g, ' ').toLowerCase()}{p.cardLast4 ? ` ·· ${p.cardLast4}` : ''} · {dateTimeFmt(p.createdAt)}</span>
                  <span><b>{money(p.amount)}</b> <span className={`pill pill-pay-${p.status.toLowerCase()}`}>{p.status}</span>{p.failureReason ? ` ${p.failureReason}` : ''}</span>
                </div>
              ))}
            </section>
          )}
          {order.status === 'PENDING_PAYMENT' && (
            <RetryPayment order={order} onPaid={async (o, text, failed) => {
              setData(o); setMessage({ text, failed })
              if (!failed) await refreshUser()
            }} />
          )}
        </div>

        <aside className="card summary">
          <h3>Summary</h3>
          <div className="row-between"><span>Subtotal</span><span>{money(order.subtotal)}</span></div>
          {order.pointsRedeemed > 0 && <div className="row-between ok"><span>Gift points ({order.pointsRedeemed})</span><span>− {money(order.discount)}</span></div>}
          <div className="row-between total"><span>Total</span><b>{money(order.total)}</b></div>
          {order.pointsEarned > 0 && order.status === 'CONFIRMED' && <p className="small muted">Earned {order.pointsEarned} gift points.</p>}
          <button className="btn btn-block" disabled={busy} onClick={buyAgain}>Buy it again</button>
          {order.cancellable && (
            <>
              <button className="btn btn-danger btn-block" disabled={busy} onClick={cancel}>Cancel order</button>
              {order.cancellableUntil && <p className="small muted">You can cancel until {dateTimeFmt(order.cancellableUntil)} (48 hours after purchase).</p>}
            </>
          )}
          {!order.cancellable && order.status !== 'CANCELLED' && <p className="small muted">The 48-hour cancellation window has ended.</p>}
        </aside>
      </div>
    </div>
  )
}
