import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  getInventorySnapshot,
  listInventoryProducts,
  recordInventoryAdjustment,
  recordInventoryEntry,
} from './api'

const movement = {
  id: 'movement-1',
  productId: 'product-1',
  type: 'ENTRY' as const,
  quantity: 5,
  delta: 5,
  previousBalance: 0,
  resultingBalance: 5,
  reason: 'Compra',
  reference: null,
  actorId: 'admin-1',
  actorDisplayName: 'Administración',
  occurredAt: '2026-08-04T04:00:00Z',
}

describe('inventory API', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('lists products and reads the auditable inventory snapshot', async () => {
    const products = [
      { id: 'product-1', code: '001', name: 'Arroz', status: 'ACTIVE', currentStock: 5 },
    ]
    const snapshot = {
      productId: 'product-1',
      productCode: '001',
      productName: 'Arroz',
      currentBalance: 5,
      version: 1,
      movements: { items: [movement], page: 0, size: 100, totalElements: 1, totalPages: 1 },
    }
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        response({
          items: products,
          page: 0,
          size: 100,
          totalElements: 1,
          totalPages: 1,
        }),
      )
      .mockResolvedValueOnce(response(snapshot))
    vi.stubGlobal('fetch', fetchMock)

    await expect(listInventoryProducts()).resolves.toEqual(products)
    await expect(getInventorySnapshot('product-1')).resolves.toEqual(snapshot)
    expect(fetchMock).toHaveBeenNthCalledWith(
      1,
      '/api/v1/admin/catalog/products?includeInactive=true&page=0&size=100',
      { credentials: 'same-origin' },
    )
    expect(fetchMock).toHaveBeenNthCalledWith(
      2,
      '/api/v1/admin/inventory/products/product-1?page=0&size=100',
      { credentials: 'same-origin' },
    )
  })

  it('records entries and adjustments through CSRF-protected mutations', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(csrf('entry-token'))
      .mockResolvedValueOnce(response(movement))
      .mockResolvedValueOnce(csrf('adjustment-token'))
      .mockResolvedValueOnce(response({ ...movement, type: 'ADJUSTMENT_OUT', delta: -1 }))
    vi.stubGlobal('fetch', fetchMock)

    await recordInventoryEntry('product-1', 5, ' Compra ', ' ')
    await recordInventoryAdjustment('product-1', 'NEGATIVE', 1, 'Merma')

    expect(fetchMock).toHaveBeenNthCalledWith(
      2,
      '/api/v1/admin/inventory/entries',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          productId: 'product-1',
          quantity: 5,
          reason: 'Compra',
          reference: null,
        }),
      }),
    )
    expect(fetchMock).toHaveBeenNthCalledWith(
      4,
      '/api/v1/admin/inventory/adjustments',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          productId: 'product-1',
          direction: 'NEGATIVE',
          quantity: 1,
          reason: 'Merma',
        }),
      }),
    )
  })
})

function csrf(token: string): Response {
  return response({ headerName: 'X-XSRF-TOKEN', token })
}

function response(body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
  })
}
