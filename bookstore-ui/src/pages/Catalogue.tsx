import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { api } from '../api'
import { useAsync } from '../hooks'
import { useStore } from '../store'
import ProductCard from '../components/ProductCard'
import { Empty, ErrorBox, Loading } from '../components/Status'

const SORTS: [string, string][] = [
  ['TITLE', 'Title A–Z'], ['PRICE_ASC', 'Price: low to high'], ['PRICE_DESC', 'Price: high to low'],
  ['RATING', 'Best rated'], ['NEWEST', 'Newest'],
]

export default function Catalogue() {
  const { user } = useStore()
  const [sp, setSp] = useSearchParams()
  const q = sp.get('q') ?? ''
  const categoryId = sp.get('category') ? Number(sp.get('category')) : undefined
  const brandId = sp.get('brand') ? Number(sp.get('brand')) : undefined
  const sort = sp.get('sort') ?? 'TITLE'
  const inStock = sp.get('inStock') === '1'
  const page = Number(sp.get('page') ?? 0)

  const [search, setSearch] = useState(q)
  useEffect(() => setSearch(q), [q])

  const cats = useAsync(() => api.categories(), [])
  const brands = useAsync(() => api.brands(), [])
  const products = useAsync(
    () => api.products({ q, categoryId, brandId, sort, inStock: inStock || undefined, page, size: 12 }),
    [q, categoryId, brandId, sort, inStock, page],
  )
  const recs = useAsync(() => (user ? api.recommendations(6) : Promise.resolve([])), [user?.id])

  const update = (changes: Record<string, string | undefined>) => {
    const next = new URLSearchParams(sp)
    for (const [k, v] of Object.entries(changes)) v ? next.set(k, v) : next.delete(k)
    if (!('page' in changes)) next.delete('page')
    setSp(next)
  }

  return (
    <div className="layout">
      <aside className="sidebar">
        <h3>Categories</h3>
        <ul className="filter-list">
          <li><button className={!categoryId ? 'on' : ''} onClick={() => update({ category: undefined })}>All categories</button></li>
          {cats.data?.map(c => (
            <li key={c.id}><button className={categoryId === c.id ? 'on' : ''} onClick={() => update({ category: String(c.id) })}>{c.name}</button></li>
          ))}
        </ul>
        <h3>Brands</h3>
        <ul className="filter-list">
          <li><button className={!brandId ? 'on' : ''} onClick={() => update({ brand: undefined })}>All brands</button></li>
          {brands.data?.map(b => (
            <li key={b.id}><button className={brandId === b.id ? 'on' : ''} onClick={() => update({ brand: String(b.id) })}>{b.name}</button></li>
          ))}
        </ul>
        <label className="check">
          <input type="checkbox" checked={inStock} onChange={e => update({ inStock: e.target.checked ? '1' : undefined })} /> In stock only
        </label>
      </aside>

      <section className="content">
        {user && recs.data && recs.data.length > 0 && !q && !categoryId && !brandId && page === 0 && (
          <div className="recs">
            <h2>Recommended for you</h2>
            <div className="grid">
              {recs.data.slice(0, 4).map(r => <ProductCard key={r.product.id} p={r.product} reason={r.reason} />)}
            </div>
          </div>
        )}

        <div className="toolbar">
          <form className="search" onSubmit={e => { e.preventDefault(); update({ q: search.trim() || undefined }) }}>
            <input type="search" placeholder="Search title, author or ISBN" value={search} onChange={e => setSearch(e.target.value)} />
            <button className="btn btn-primary">Search</button>
          </form>
          <label className="sort">Sort
            <select value={sort} onChange={e => update({ sort: e.target.value })}>
              {SORTS.map(([v, l]) => <option key={v} value={v}>{l}</option>)}
            </select>
          </label>
        </div>

        {products.loading && !products.data && <Loading text="Loading books…" />}
        {products.error && <ErrorBox error={products.error} retry={products.reload} />}
        {products.data && (
          <>
            <p className="muted small">{products.data.totalElements} book{products.data.totalElements === 1 ? '' : 's'}</p>
            {products.data.content.length === 0
              ? <Empty>No books match your filters.</Empty>
              : <div className="grid">{products.data.content.map(p => <ProductCard key={p.id} p={p} />)}</div>}
            {products.data.totalPages > 1 && (
              <div className="pager">
                <button className="btn" disabled={page === 0} onClick={() => update({ page: String(page - 1) })}>← Previous</button>
                <span className="muted">Page {page + 1} of {products.data.totalPages}</span>
                <button className="btn" disabled={page + 1 >= products.data.totalPages} onClick={() => update({ page: String(page + 1) })}>Next →</button>
              </div>
            )}
          </>
        )}
      </section>
    </div>
  )
}
