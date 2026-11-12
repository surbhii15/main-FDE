import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api, dateFmt, errMsg, money } from '../api'
import { useStore } from '../store'
import type { Product } from '../types'

const PALETTE = ['#1f4d3a', '#7a3b2e', '#2b4a7a', '#5b3a7a', '#7a6a2b', '#2e6b6b', '#8a3b5c', '#44506b']

function hue(s: string) {
  let h = 0
  for (const c of s) h = (h * 31 + c.charCodeAt(0)) >>> 0
  return PALETTE[h % PALETTE.length]
}

/**
 * Book cover. Uses the product image when it loads; otherwise (no image, or the image host is
 * unreachable) draws a title cover locally so the catalogue never shows broken images.
 */
export function Cover({ p, size = 'card' }: { p: { imageUrl?: string; title: string; author?: string }; size?: 'card' | 'large' | 'thumb' }) {
  // The generated cover is always drawn; the real image fades in over it only once it has loaded.
  // (A request to an unreachable host can hang for a long time without ever firing onError.)
  const [loaded, setLoaded] = useState(false)
  return (
    <div className={`cover cover-${size} cover-fallback`} style={{ background: `linear-gradient(160deg, ${hue(p.title)}, #14171c)` }} role="img" aria-label={p.title}>
      {size !== 'thumb' && (
        <>
          <span className="cover-title">{p.title}</span>
          {p.author && <span className="cover-author">{p.author}</span>}
        </>
      )}
      {p.imageUrl && (
        <img className={`cover-img ${loaded ? 'cover-img-on' : ''}`} src={p.imageUrl} alt="" loading="lazy" onLoad={() => setLoaded(true)} />
      )}
    </div>
  )
}

export function Stars({ value }: { value?: number }) {
  if (value == null) return null
  return <span className="stars" title={`${value} out of 5`}>★ {value.toFixed(1)}</span>
}

export function useAddToCart() {
  const { user, setCart, notify } = useStore()
  const nav = useNavigate()
  return async (p: Product, quantity = 1) => {
    if (!user) { nav('/login', { state: { from: `/products/${p.id}` } }); return }
    try {
      setCart(await api.addToCart(p.id, quantity))
      notify(`Added "${p.title}" to your cart`)
    } catch (e) {
      notify(errMsg(e), 'error')
    }
  }
}

export default function ProductCard({ p, reason }: { p: Product; reason?: string }) {
  const add = useAddToCart()
  return (
    <article className="card product-card">
      <Link to={`/products/${p.id}`}><Cover p={p} /></Link>
      <div className="card-body">
        <Link to={`/products/${p.id}`} className="title">{p.title}</Link>
        <div className="muted small">{p.author}</div>
        <div className="row-between">
          <b>{money(p.price)}</b>
          <Stars value={p.rating} />
        </div>
        {reason && <div className="reason small">{reason}</div>}
        {p.inStock
          ? <div className="small ok">Delivery by {dateFmt(p.estimatedDeliveryDate)}</div>
          : <div className="small bad">Out of stock</div>}
        <button className="btn btn-primary" disabled={!p.inStock} onClick={() => add(p)}>Add to cart</button>
      </div>
    </article>
  )
}
