import { useEffect } from 'react'
import { Link } from 'react-router-dom'
import { api, dateFmt, errMsg, money } from '../api'
import { useAsync } from '../hooks'
import { useStore } from '../store'
import ProductCard, { Cover } from '../components/ProductCard'
import { Empty } from '../components/Status'

export default function CartPage() {
  const { cart, setCart, refreshCart, notify } = useStore()
  const recs = useAsync(() => api.recommendations(4), [cart?.itemCount])

  useEffect(() => { void refreshCart() }, [refreshCart])

  const run = async (fn: () => Promise<unknown>) => {
    try { await fn() } catch (e) { notify(errMsg(e), 'error') }
  }
  const setQty = (id: number, q: number) => run(async () => setCart(await api.setQuantity(id, q)))
  const remove = (id: number) => run(async () => setCart(await api.removeFromCart(id)))

  return (
    <div>
      <h1>Your cart</h1>
      {!cart || cart.items.length === 0 ? (
        <Empty>Your cart is empty. <Link to="/">Browse books</Link></Empty>
      ) : (
        <div className="cart-layout">
          <div className="card">
            {cart.items.map(i => (
              <div key={i.productId} className="cart-row">
                <Link to={`/products/${i.productId}`}><Cover p={i} size="thumb" /></Link>
                <div className="grow">
                  <Link to={`/products/${i.productId}`} className="title">{i.title}</Link>
                  <div className="muted small">{i.author}</div>
                  <div className="small">Delivery by {dateFmt(i.estimatedDeliveryDate)}</div>
                  {!i.available && <div className="bad small">Only {i.stockQuantity} in stock. Reduce the quantity to check out.</div>}
                </div>
                <div className="qty-box">
                  <button className="btn" aria-label="Decrease" onClick={() => setQty(i.productId, i.quantity - 1)}>−</button>
                  <span>{i.quantity}</span>
                  <button className="btn" aria-label="Increase" disabled={i.quantity >= i.stockQuantity} onClick={() => setQty(i.productId, i.quantity + 1)}>+</button>
                </div>
                <b className="line-total">{money(i.lineTotal)}</b>
                <button className="btn btn-link" onClick={() => remove(i.productId)}>Remove</button>
              </div>
            ))}
          </div>
          <aside className="card summary">
            <h3>Summary</h3>
            <div className="row-between"><span>Items</span><span>{cart.itemCount}</span></div>
            <div className="row-between"><span>Subtotal</span><b>{money(cart.subtotal)}</b></div>
            {cart.estimatedDeliveryDate && <p className="small muted">Whole order delivered by {dateFmt(cart.estimatedDeliveryDate)}</p>}
            <Link to="/checkout" className={`btn btn-primary btn-block ${cart.readyForCheckout ? '' : 'disabled'}`} aria-disabled={!cart.readyForCheckout}
              onClick={e => { if (!cart.readyForCheckout) e.preventDefault() }}>Proceed to checkout</Link>
          </aside>
        </div>
      )}

      {recs.data && recs.data.length > 0 && (
        <section>
          <h2>You might also like</h2>
          <div className="grid">{recs.data.map(r => <ProductCard key={r.product.id} p={r.product} reason={r.reason} />)}</div>
        </section>
      )}
    </div>
  )
}
