import { afterEach, describe, expect, it, vi } from 'vitest'
import { cancelSale, confirmSale, listRecentSales, type Sale } from './api'

describe('sales API', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('confirms a sale with a CSRF token and an idempotency key', async () => {
    const sale = saleFixture()
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(response({ headerName: 'X-XSRF-TOKEN', token: 'csrf' }))
      .mockResolvedValueOnce(response(sale, 201))
    vi.stubGlobal('fetch', fetchMock)

    await expect(
      confirmSale({
        idempotencyKey: 'key-1',
        lines: [{ productId: 'product-1', quantity: 2 }],
        payment: { method: 'CASH', cashReceived: 5000 },
      }),
    ).resolves.toEqual(sale)

    expect(fetchMock).toHaveBeenNthCalledWith(
      2,
      '/api/v1/sales',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          idempotencyKey: 'key-1',
          lines: [{ productId: 'product-1', quantity: 2 }],
          payment: { method: 'CASH', cashReceived: 5000 },
        }),
      }),
    )
  })

  it('loads recent sale records', async () => {
    const page = { items: [saleFixture()], page: 0, size: 20, totalElements: 1, totalPages: 1 }
    vi.stubGlobal('fetch', vi.fn().mockResolvedValueOnce(response(page)))

    await expect(listRecentSales()).resolves.toEqual(page)
    expect(fetch).toHaveBeenCalledWith('/api/v1/sales?page=0&size=20', {
      credentials: 'same-origin',
    })
  })

  it('cancels a sale through the administrative endpoint', async () => {
    const cancelled = { ...saleFixture(), status: 'VOIDED' as const }
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(response({ headerName: 'X-XSRF-TOKEN', token: 'csrf' }))
      .mockResolvedValueOnce(response(cancelled, 201))
    vi.stubGlobal('fetch', fetchMock)

    await expect(
      cancelSale('sale-1', { refundMethod: 'CASH', reason: 'Error de cobro' }),
    ).resolves.toEqual(cancelled)
    expect(fetchMock).toHaveBeenNthCalledWith(
      2,
      '/api/v1/admin/sales/sale-1/cancellations',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ refundMethod: 'CASH', reason: 'Error de cobro' }),
      }),
    )
  })
})

function saleFixture(): Sale {
  return {
    id: 'sale-1',
    folio: 'V-000001',
    cashSessionId: 'cash-1',
    actorId: 'admin-1',
    actorDisplayName: 'Administración',
    confirmedAt: '2026-08-05T15:00:00Z',
    status: 'CONFIRMED',
    total: 3000,
    lines: [
      {
        productId: 'product-1',
        productName: 'Arroz',
        productCode: '001',
        quantity: 2,
        unitSalePrice: 1500,
        subtotal: 3000,
      },
    ],
    payment: {
      method: 'CASH',
      amount: 3000,
      cashPayable: 3000,
      roundingAdjustment: 0,
      cashReceived: 5000,
      change: 2000,
    },
    cancellation: null,
    notice: 'Comprobante interno no tributario.',
  }
}

function response(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}
