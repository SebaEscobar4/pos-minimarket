import { mutateJson, readJson } from '../../shared/api/client'

export type InventoryProduct = {
  id: string
  code: string | null
  name: string
  status: 'ACTIVE' | 'INACTIVE'
  currentStock: number
}

export type InventoryMovementType =
  | 'ENTRY'
  | 'SALE_OUT'
  | 'ADJUSTMENT_IN'
  | 'ADJUSTMENT_OUT'
  | 'DAMAGED'
  | 'EXPIRED'
  | 'SALE_REVERSAL'

export type InventoryMovement = {
  id: string
  productId: string
  type: InventoryMovementType
  quantity: number
  delta: number
  previousBalance: number
  resultingBalance: number
  reason: string | null
  reference: string | null
  actorId: string
  actorDisplayName: string
  occurredAt: string
}

export type InventorySnapshot = {
  productId: string
  productCode: string | null
  productName: string
  currentBalance: number
  version: number
  movements: PageResponse<InventoryMovement>
}

type PageResponse<T> = {
  items: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export async function listInventoryProducts(): Promise<InventoryProduct[]> {
  const response = await fetch(
    '/api/v1/admin/catalog/products?includeInactive=true&page=0&size=100',
    { credentials: 'same-origin' },
  )
  return (await readJson<PageResponse<InventoryProduct>>(response)).items
}

export async function getInventorySnapshot(productId: string): Promise<InventorySnapshot> {
  return readJson(
    await fetch(`/api/v1/admin/inventory/products/${productId}?page=0&size=100`, {
      credentials: 'same-origin',
    }),
  )
}

export function recordInventoryEntry(
  productId: string,
  quantity: number,
  reason: string,
  reference: string,
): Promise<InventoryMovement> {
  return mutateJson('/api/v1/admin/inventory/entries', 'POST', {
    productId,
    quantity,
    reason: reason.trim() || null,
    reference: reference.trim() || null,
  })
}

export function recordInventoryAdjustment(
  productId: string,
  direction: 'POSITIVE' | 'NEGATIVE',
  quantity: number,
  reason: string,
): Promise<InventoryMovement> {
  return mutateJson('/api/v1/admin/inventory/adjustments', 'POST', {
    productId,
    direction,
    quantity,
    reason,
  })
}
