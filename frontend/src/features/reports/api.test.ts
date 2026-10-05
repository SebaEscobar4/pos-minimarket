import { afterEach, describe, expect, it, vi } from 'vitest'
import { getCashReport, getInventoryReport, getSalesReport } from './api'

describe('reports API', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('encodes sales, inventory and cash filters', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockImplementation(() => Promise.resolve(response({}))),
    )

    await getSalesReport({ from: '2026-08-01', to: '2026-08-05', method: 'CASH' })
    await getInventoryReport({
      from: '2026-08-01',
      to: '2026-08-05',
      type: 'SALE_OUT',
      productId: 'product-1',
      page: 2,
      size: 20,
    })
    await getCashReport({ from: '2026-08-01', to: '2026-08-05', page: 1, size: 20 })

    expect(fetch).toHaveBeenNthCalledWith(
      1,
      '/api/v1/admin/reports/sales?from=2026-08-01&to=2026-08-05&method=CASH',
      { credentials: 'same-origin' },
    )
    expect(fetch).toHaveBeenNthCalledWith(
      2,
      '/api/v1/admin/reports/inventory?from=2026-08-01&to=2026-08-05&type=SALE_OUT&productId=product-1&page=2&size=20',
      { credentials: 'same-origin' },
    )
    expect(fetch).toHaveBeenNthCalledWith(
      3,
      '/api/v1/admin/reports/cash?from=2026-08-01&to=2026-08-05&page=1&size=20',
      { credentials: 'same-origin' },
    )
  })
})

function response(body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
  })
}
