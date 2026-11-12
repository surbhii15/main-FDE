import type { ReactNode } from 'react'
import type { OrderStatus } from '../types'

export function ErrorBox({ error, retry }: { error: Error; retry?: () => void }) {
  return (
    <div className="alert alert-error">
      {error.message}
      {retry && <button className="btn btn-link" onClick={retry}>Retry</button>}
    </div>
  )
}

export function Loading({ text = 'Loading…' }: { text?: string }) {
  return <div className="center muted">{text}</div>
}

export function Empty({ children }: { children: ReactNode }) {
  return <div className="center muted empty">{children}</div>
}

const LABEL: Record<OrderStatus, string> = {
  PENDING_PAYMENT: 'Awaiting payment',
  CONFIRMED: 'Confirmed',
  CANCELLED: 'Cancelled',
}

export function StatusPill({ status }: { status: OrderStatus }) {
  return <span className={`pill pill-${status.toLowerCase()}`}>{LABEL[status]}</span>
}
