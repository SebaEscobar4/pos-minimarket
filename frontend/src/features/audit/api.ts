import { readJson } from '../../shared/api/client'

export type AuditEventType =
  | 'LOGIN_SUCCEEDED'
  | 'LOGIN_FAILED'
  | 'LOGIN_RATE_LIMITED'
  | 'PASSWORD_CHANGED'
  | 'LOGOUT_SUCCEEDED'
  | 'CASH_OPENED'
  | 'CASH_CLOSED'
  | 'CASH_SALE'
  | 'MANUAL_INCOME'
  | 'MANUAL_WITHDRAWAL'
  | 'CASH_REFUND'
  | 'INVENTORY_ENTRY'
  | 'INVENTORY_SALE_OUT'
  | 'INVENTORY_ADJUSTMENT_IN'
  | 'INVENTORY_ADJUSTMENT_OUT'
  | 'INVENTORY_DAMAGED'
  | 'INVENTORY_EXPIRED'
  | 'INVENTORY_SALE_REVERSAL'
  | 'SALE_CONFIRMED'
  | 'SALE_VOIDED'

export type AuditEvent = {
  id: string
  type: AuditEventType
  occurredAt: string
  actorId: string | null
  actorDisplayName: string
  reference: string | null
  summary: string
}

export type AuditEventPage = {
  items: AuditEvent[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type AuditFilters = {
  from?: string
  to?: string
  type?: AuditEventType
  reference?: string
  page?: number
  size?: number
}

export async function listAuditEvents(filters: AuditFilters = {}): Promise<AuditEventPage> {
  const parameters = new URLSearchParams()
  if (filters.from) parameters.set('from', filters.from)
  if (filters.to) parameters.set('to', filters.to)
  if (filters.type) parameters.set('type', filters.type)
  if (filters.reference?.trim()) parameters.set('reference', filters.reference.trim())
  parameters.set('page', String(filters.page ?? 0))
  parameters.set('size', String(filters.size ?? 20))
  return readJson(
    await fetch(`/api/v1/admin/audit-events?${parameters.toString()}`, {
      credentials: 'same-origin',
    }),
  )
}
