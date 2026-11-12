import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api, dateFmt, money } from '../api'
import { useAsync } from '../hooks'
import ProductCard, { Cover, Stars, useAddToCart } from '../components/ProductCard'
import { ErrorBox, Loading } from '../components/Status'

export default function ProductPage() {
  const id = Number(useParams().id)
  const { data, error, loading, reload } = useAsync(() => api.product(id), [id])
  const [qty, setQty] = useState(1)
  const add = useAddToCart()

  if (loading && !data) return <Loading />
  if (error) return <ErrorBox error={error} retry={reload} />
  if (!data) return null
  const p = data.product

  return (
    <div>
      <p className="small"><Link to="/">← All books</Link> · <Link to={`/?category=${p.category.id}`}>{p.category.name}</Link></p>
      <div className="product-detail">
        <Cover p={p} size="large" />
        <div>
          <h1>{p.title}</h1>
          <p className="muted">by <Link to={`/?q=${encodeURIComponent(p.author)}`}>{p.author}</Link> · <Link to={`/?brand=${p.brand.id}`}>{p.brand.name}</Link></p>
          <Stars value={p.rating} />
          <p className="price">{money(p.price)}</p>
          {p.inStock ? (
            <p className="ok">
              In stock{p.stockQuantity <= 5 ? ` — only ${p.stockQuantity} left` : ''}.{' '}
              <b>Estimated delivery: {dateFmt(p.estimatedDeliveryDate)}</b>
            </p>
          ) : <p className="bad">Currently out of stock</p>}
          <p>{p.description}</p>
          <p className="muted small">ISBN {p.isbn} · SKU {p.sku}</p>
          <div className="row-gap">
            <label className="qty">Qty
              <input type="number" min={1} max={Math.max(1, Math.min(99, p.stockQuantity))} value={qty}
                onChange={e => setQty(Math.max(1, Number(e.target.value) || 1))} />
            </label>
            <button className="btn btn-primary" disabled={!p.inStock} onClick={() => add(p, qty)}>Add to cart</button>
          </div>
        </div>
      </div>

      {data.relatedProducts.length > 0 && (
        <section>
          <h2>Related books</h2>
          <div className="grid">{data.relatedProducts.map(r => <ProductCard key={r.id} p={r} />)}</div>
        </section>
      )}
    </div>
  )
}
