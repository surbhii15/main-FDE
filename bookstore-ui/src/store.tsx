import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api, setUnauthorizedHandler, tokenStore } from './api'
import type { Cart, User } from './types'

interface Toast { id: number; kind: 'ok' | 'error'; text: string }

interface Store {
  user: User | null
  ready: boolean
  cart: Cart | null
  toasts: Toast[]
  signIn: (token: string, user: User) => void
  signOut: () => void
  refreshUser: () => Promise<void>
  setCart: (c: Cart | null) => void
  refreshCart: () => Promise<void>
  notify: (text: string, kind?: 'ok' | 'error') => void
}

const Ctx = createContext<Store | null>(null)

export function StoreProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [ready, setReady] = useState(false)
  const [cart, setCart] = useState<Cart | null>(null)
  const [toasts, setToasts] = useState<Toast[]>([])

  const notify = useCallback((text: string, kind: 'ok' | 'error' = 'ok') => {
    const id = Date.now() + Math.random()
    setToasts(t => [...t, { id, kind, text }])
    setTimeout(() => setToasts(t => t.filter(x => x.id !== id)), 4500)
  }, [])

  const signOut = useCallback(() => {
    tokenStore.clear()
    setUser(null)
    setCart(null)
  }, [])

  const refreshCart = useCallback(async () => {
    try { setCart(await api.cart()) } catch { /* shown by the caller if it matters */ }
  }, [])

  const refreshUser = useCallback(async () => {
    try { setUser(await api.me()) } catch { /* handled by unauthorized handler */ }
  }, [])

  const signIn = useCallback((token: string, u: User) => {
    tokenStore.set(token)
    setUser(u)
    void refreshCart()
  }, [refreshCart])

  useEffect(() => {
    setUnauthorizedHandler(() => { signOut(); notify('Your session expired. Please sign in again.', 'error') })
  }, [signOut, notify])

  useEffect(() => {
    if (!tokenStore.get()) { setReady(true); return }
    api.me()
      .then(u => { setUser(u); return api.cart().then(setCart) })
      .catch(() => tokenStore.clear())
      .finally(() => setReady(true))
  }, [])

  const value = useMemo(
    () => ({ user, ready, cart, toasts, signIn, signOut, refreshUser, setCart, refreshCart, notify }),
    [user, ready, cart, toasts, signIn, signOut, refreshUser, refreshCart, notify],
  )
  return <Ctx.Provider value={value}>{children}</Ctx.Provider>
}

export function useStore() {
  const v = useContext(Ctx)
  if (!v) throw new Error('useStore must be used inside StoreProvider')
  return v
}
