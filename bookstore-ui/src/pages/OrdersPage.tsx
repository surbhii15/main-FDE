import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api, dateFmt, errMsg, money } from '../api'
import { useAsync } from '../hooks'
import { useStore } from '../store'
import { Empty, ErrorBox, Loading, StatusPill } from '../components/Status'

export default function OrdersPage() {
  const [page, setPage] = useState(0)
  const { data, error, loading, reload } = useAsync(() => api.orders(page), [page])
  const { setCart, notify } = useStore()
  const nav = useNavigate()

  async function buyAgain(id: number) {
    try {
      const r = await api.buyAgain(id)
      setCart(r.cart)
      notify(r.skipped.length ? `Added ${r.added.length} item(s); skipped: ${r.skipped.join(', ')}` : 'Items added to your cart')
      nav('/cart')
    } catch (e) { notify(errMsg(e), 'error') }
  }

  return (
    <div>
      <h1>My orders</h1>
      {loading && !data && <Loading />}
      {error && <ErrorBox error={error} retry={reload} />}
      {data && data.content.length === 0 && <Empty>No orders yet. <Link to="/">Start shopping</Link></Empty>}
      {data?.content.map(o => (
        <div key={o.id} className="card pad order-row">
          <div className="grow">
            <div className="row-gap">
              <Link to={`/orders/${o.id}`} className="title">{o.orderNumber}</Link>
              <StatusPill status={o.status} />
            </div>
            <div className="muted small">Placed {dateFmt(o.placedAt)} · {o.itemCount} item{o.itemCount === 1 ? '' : 's'}</div>
            <div className="small">{o.itemTitles.join(', ')}{o.itemCount > o.itemTitles.length ? '…' : ''}</div>
            {o.status !== 'CANCELLED' && <div className="small muted">Delivery by {dateFmt(o.estimatedDeliveryDate)}{o.cancellable && ' · can be cancelled'}</div>}
          </div>
          <b>{money(o.total)}</b>
          <div className="stack-sm">
            <button className="btn" onClick={() => buyAgain(o.id)}>Buy it again</button>
            <Link className="btn btn-link" to={`/orders/${o.id}`}>View details</Link>
          </div>
        </div>
      ))}
      {data && data.totalPages > 1 && (
        <div className="pager">
          <button className="btn" disabled={page === 0} onClick={() => setPage(page - 1)}>← Newer</button>
          <span className="muted">Page {page + 1} of {data.totalPages}</span>
          <button className="btn" disabled={page + 1 >= data.totalPages} onClick={() => setPage(page + 1)}>Older →</button>
        </div>
      )}
    </div>
  )
}
