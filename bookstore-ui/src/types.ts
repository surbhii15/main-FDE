export type Role = 'CUSTOMER' | 'ADMIN'

export interface User {
  id: number
  email: string
  fullName: string
  phone?: string
  role: Role
  giftPoints: number
}

export interface AuthResponse {
  accessToken: string
  tokenType: string
  expiresInSeconds: number
  user: User
}

export interface Category {
  id: number
  name: string
  slug: string
  description?: string
}

export interface Brand {
  id: number
  name: string
  description?: string
}

export interface Product {
  id: number
  sku: string
  title: string
  author: string
  isbn?: string
  description?: string
  price: number
  stockQuantity: number
  inStock: boolean
  deliveryDays: number
  estimatedDeliveryDate: string
  rating?: number
  imageUrl?: string
  category: { id: number; name: string }
  brand: { id: number; name: string }
}

export interface ProductDetail {
  product: Product
  relatedProducts: Product[]
}

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface CartItem {
  productId: number
  sku: string
  title: string
  author: string
  imageUrl?: string
  unitPrice: number
  quantity: number
  lineTotal: number
  available: boolean
  stockQuantity: number
  estimatedDeliveryDate: string
}

export interface Cart {
  items: CartItem[]
  itemCount: number
  subtotal: number
  estimatedDeliveryDate?: string
  readyForCheckout: boolean
}

export interface BuyAgainResult {
  cart: Cart
  added: string[]
  skipped: string[]
}

export interface Address {
  id: number
  label?: string
  recipientName: string
  line1: string
  line2?: string
  city: string
  state?: string
  postalCode: string
  country: string
  phone?: string
  isDefault: boolean
}

export type AddressInput = Omit<Address, 'id' | 'isDefault'> & { isDefault?: boolean }

export type PaymentMethod = 'CREDIT_CARD' | 'DEBIT_CARD' | 'UPI' | 'NET_BANKING' | 'CASH_ON_DELIVERY'

export interface PaymentMethodOption {
  method: PaymentMethod
  label: string
  requires: string
}

export interface CardDetails {
  number: string
  holderName: string
  expiryMonth: number
  expiryYear: number
  cvv: string
}

export interface PaymentRequest {
  method: PaymentMethod
  card?: CardDetails
  upiId?: string
  bankCode?: string
}

export interface CheckoutPreview {
  cart: Cart
  subtotal: number
  giftPointBalance: number
  maxRedeemablePoints: number
  pointsToRedeem: number
  discount: number
  total: number
  pointsToEarn: number
  paymentMethods: PaymentMethodOption[]
}

export type OrderStatus = 'PENDING_PAYMENT' | 'CONFIRMED' | 'CANCELLED'

export interface Payment {
  id: number
  method: PaymentMethod
  status: 'SUCCESS' | 'PENDING' | 'FAILED' | 'REFUNDED' | 'CANCELLED'
  amount: number
  cardBrand?: string
  cardLast4?: string
  transactionRef?: string
  failureReason?: string
  createdAt: string
}

export interface Order {
  id: number
  orderNumber: string
  status: OrderStatus
  placedAt: string
  items: { productId: number; title: string; unitPrice: number; quantity: number; lineTotal: number }[]
  subtotal: number
  pointsRedeemed: number
  discount: number
  total: number
  pointsEarned: number
  shippingAddress: Omit<Address, 'id' | 'isDefault' | 'label'>
  estimatedDeliveryDate: string
  payments: Payment[]
  cancellable: boolean
  cancellableUntil?: string
  cancelledAt?: string
  message: string
}

export interface OrderSummary {
  id: number
  orderNumber: string
  status: OrderStatus
  placedAt: string
  itemCount: number
  itemTitles: string[]
  total: number
  estimatedDeliveryDate: string
  cancellable: boolean
}

export interface PaymentResult {
  payment: Payment
  order: Order
  message: string
}

export interface Recommendation {
  product: Product
  reason: string
}

export interface PointsSummary {
  balance: number
  maxRedeemPercent: number
  earnPerAmount: number
  recentTransactions: { id: number; orderId?: number; points: number; type: string; description?: string; createdAt: string }[]
}
