import type {
  Address, AddressInput, AuthResponse, Brand, BuyAgainResult, Cart, Category, CheckoutPreview, Order,
  OrderStatus, OrderSummary, Page, PaymentRequest, PaymentResult, PointsSummary, Product, ProductDetail,
  Recommendation, User,
} from './types'

const TOKEN_KEY = 'bookstore.token'

export const tokenStore = {
  get: () => { try { return localStorage.getItem(TOKEN_KEY) } catch { return null } },
  set: (t: string) => { try { localStorage.setItem(TOKEN_KEY, t) } catch { /* private mode */ } },
  clear: () => { try { localStorage.removeItem(TOKEN_KEY) } catch { /* private mode */ } },
}

/** Mirrors the backend's JSON error body. */
export class ApiError extends Error {
  status: number
  code: string
  fieldErrors?: Record<string, string>
  constructor(status: number, code: string, message: string, fieldErrors?: Record<string, string>) {
    super(message)
    this.status = status
    this.code = code
    this.fieldErrors = fieldErrors
  }
}

let onUnauthorized: () => void = () => {}
export const setUnauthorizedHandler = (fn: () => void) => { onUnauthorized = fn }

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  const token = tokenStore.get()
  if (token) headers.Authorization = `Bearer ${token}`

  let res: Response
  try {
    res = await fetch(path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) })
  } catch {
    throw new ApiError(0, 'NETWORK', 'Cannot reach the server. Is the backend running on port 8080?')
  }
  if (res.status === 204) return undefined as T
  const text = await res.text()
  const data = text ? JSON.parse(text) : undefined
  if (!res.ok) {
    if (res.status === 401 && token) onUnauthorized()
    throw new ApiError(res.status, data?.code ?? 'ERROR', data?.message ?? `Request failed (${res.status})`, data?.fieldErrors)
  }
  return data as T
}

const get = <T,>(p: string) => request<T>('GET', p)
const post = <T,>(p: string, b?: unknown) => request<T>('POST', p, b)
const put = <T,>(p: string, b?: unknown) => request<T>('PUT', p, b)
const del = <T,>(p: string) => request<T>('DELETE', p)

export interface ProductQuery {
  q?: string
  categoryId?: number
  brandId?: number
  sort?: string
  inStock?: boolean
  minPrice?: string
  maxPrice?: string
  page?: number
  size?: number
}

function qs(params: Record<string, string | number | boolean | undefined>) {
  const sp = new URLSearchParams()
  for (const [k, v] of Object.entries(params)) if (v !== undefined && v !== '') sp.set(k, String(v))
  const s = sp.toString()
  return s ? `?${s}` : ''
}

export const api = {
  // auth
  login: (email: string, password: string) => post<AuthResponse>('/api/auth/login', { email, password }),
  register: (r: { email: string; password: string; fullName: string; phone?: string }) =>
    post<AuthResponse>('/api/auth/register', r),
  me: () => get<User>('/api/users/me'),
  points: () => get<PointsSummary>('/api/users/me/gift-points'),

  // catalogue
  categories: () => get<Category[]>('/api/categories'),
  brands: () => get<Brand[]>('/api/brands'),
  products: (q: ProductQuery) => get<Page<Product>>('/api/products' + qs({ ...q })),
  product: (id: number) => get<ProductDetail>(`/api/products/${id}`),
  recommendations: (limit = 6) => get<Recommendation[]>(`/api/recommendations?limit=${limit}`),

  // cart
  cart: () => get<Cart>('/api/cart'),
  addToCart: (productId: number, quantity = 1) => post<Cart>('/api/cart/items', { productId, quantity }),
  setQuantity: (productId: number, quantity: number) => put<Cart>(`/api/cart/items/${productId}`, { quantity }),
  removeFromCart: (productId: number) => del<Cart>(`/api/cart/items/${productId}`),

  // addresses
  addresses: () => get<Address[]>('/api/addresses'),
  addAddress: (a: AddressInput) => post<Address>('/api/addresses', a),
  deleteAddress: (id: number) => del<void>(`/api/addresses/${id}`),

  // checkout & orders
  preview: (pointsToRedeem: number) => get<CheckoutPreview>(`/api/checkout/preview?pointsToRedeem=${pointsToRedeem}`),
  placeOrder: (addressId: number, pointsToRedeem: number) => post<Order>('/api/orders', { addressId, pointsToRedeem }),
  pay: (orderId: number, p: PaymentRequest) => post<PaymentResult>(`/api/orders/${orderId}/payments`, p),
  orders: (page = 0, status?: OrderStatus) => get<Page<OrderSummary>>('/api/orders' + qs({ page, size: 10, status })),
  order: (id: number) => get<Order>(`/api/orders/${id}`),
  cancelOrder: (id: number) => post<Order>(`/api/orders/${id}/cancel`),
  buyAgain: (id: number) => post<BuyAgainResult>(`/api/orders/${id}/buy-again`),
}

export const money = (n: number) =>
  new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 }).format(n)

export const dateFmt = (iso: string) =>
  new Date(iso).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' })

export const dateTimeFmt = (iso: string) =>
  new Date(iso).toLocaleString('en-GB', { day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' })

export const errMsg = (e: unknown) => (e instanceof Error ? e.message : 'Something went wrong')
