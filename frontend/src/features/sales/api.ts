import { mutateJson, readJson } from '../../shared/api/client'

export type PaymentMethod = 'CASH' | 'CARD' | 'TRANSFER'

export type SaleLine = {
  productId: string
  productName: string
  productCode: string | null
  quantity: number
  unitSalePrice: number
  subtotal: number
}

export type SalePayment = {
  method: PaymentMethod
  amount: number
  cashPayable: number | null
  roundingAdjustment: number | null
  cashReceived: number | null
  change: number | null
}

export type SaleCancellation = {
  refundMethod: PaymentMethod
  amount: number
  cashPayable: number | null
  roundingAdjustment: number | null
  reason: string
  actorId: string
  actorDisplayName: string
  occurredAt: string
}

export type Sale = {
  id: string
  folio: string
  cashSessionId: string
  actorId: string
  actorDisplayName: string
  confirmedAt: string
  status: 'CONFIRMED' | 'VOIDED'
  total: number
  lines: SaleLine[]
  payment: SalePayment
  cancellation: SaleCancellation | null
  notice: string
}

export type SalePage = {
  items: Sale[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type ConfirmSaleInput = {
  idempotencyKey: string
  lines: { productId: string; quantity: number }[]
  payment: { method: PaymentMethod; cashReceived?: number }
}

export function confirmSale(input: ConfirmSaleInput): Promise<Sale> {
  return mutateJson('/api/v1/sales', 'POST', input)
}

export function cancelSale(
  saleId: string,
  input: { refundMethod: PaymentMethod; reason: string },
): Promise<Sale> {
  return mutateJson(`/api/v1/admin/sales/${saleId}/cancellations`, 'POST', input)
}

export async function listRecentSales(): Promise<SalePage> {
  return readJson(
    await fetch('/api/v1/sales?page=0&size=20', {
      credentials: 'same-origin',
    }),
  )
}
