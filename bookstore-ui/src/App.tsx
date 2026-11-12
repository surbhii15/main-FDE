import { Link, NavLink, Navigate, Outlet, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import { useStore } from './store'
import Catalogue from './pages/Catalogue'
import ProductPage from './pages/ProductPage'
import CartPage from './pages/CartPage'
import CheckoutPage from './pages/CheckoutPage'
import OrdersPage from './pages/OrdersPage'
import OrderPage from './pages/OrderPage'
import LoginPage from './pages/LoginPage'

function RequireAuth() {
  const { user, ready } = useStore()
  const loc = useLocation()
  if (!ready) return <div className="center muted">Loading…</div>
  if (!user) return <Navigate to="/login" replace state={{ from: loc.pathname + loc.search }} />
  return <Outlet />
}

function Header() {
  const { user, cart, signOut } = useStore()
  const nav = useNavigate()
  return (
    <header className="header">
      <div className="container header-row">
        <Link to="/" className="brand">📚 Page Turner</Link>
        <nav className="nav">
          <NavLink to="/" end>Books</NavLink>
          {user && <NavLink to="/orders">My orders</NavLink>}
          <NavLink to="/cart" className="cart-link">
            Cart{cart && cart.itemCount > 0 && <span className="badge">{cart.itemCount}</span>}
          </NavLink>
          {user ? (
            <span className="user-box">
              <span className="muted small">{user.fullName} · <b>{user.giftPoints}</b> pts</span>
              <button className="btn btn-link" onClick={() => { signOut(); nav('/') }}>Sign out</button>
            </span>
          ) : (
            <NavLink to="/login">Sign in</NavLink>
          )}
        </nav>
      </div>
    </header>
  )
}

function Toasts() {
  const { toasts } = useStore()
  return (
    <div className="toasts" aria-live="polite">
      {toasts.map(t => <div key={t.id} className={`toast toast-${t.kind}`}>{t.text}</div>)}
    </div>
  )
}

export default function App() {
  return (
    <>
      <Header />
      <main className="container main">
        <Routes>
          <Route path="/" element={<Catalogue />} />
          <Route path="/products/:id" element={<ProductPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route element={<RequireAuth />}>
            <Route path="/cart" element={<CartPage />} />
            <Route path="/checkout" element={<CheckoutPage />} />
            <Route path="/orders" element={<OrdersPage />} />
            <Route path="/orders/:id" element={<OrderPage />} />
          </Route>
          <Route path="*" element={<div className="center"><h2>Page not found</h2><Link to="/">Back to books</Link></div>} />
        </Routes>
      </main>
      <footer className="footer"><div className="container muted small">Capstone demo bookstore · API docs at <a href="http://localhost:8080/swagger-ui.html" target="_blank" rel="noreferrer">localhost:8080/swagger-ui.html</a></div></footer>
      <Toasts />
    </>
  )
}
