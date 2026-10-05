import { afterEach, describe, expect, it, vi } from 'vitest'
import { listAuditEvents } from './api'

describe('audit API', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('loads an encoded administrative audit query', async () => {
    const page = { items: [], page: 1, size: 20, totalElements: 0, totalPages: 0 }
    vi.stubGlobal('fetch', vi.fn().mockResolvedValueOnce(response(page)))

    await expect(
      listAuditEvents({
        from: '2026-08-01',
        to: '2026-08-05',
        type: 'SALE_VOIDED',
        reference: '  V-000001  ',
        page: 1,
      }),
    ).resolves.toEqual(page)

    expect(fetch).toHaveBeenCalledWith(
      '/api/v1/admin/audit-events?from=2026-08-01&to=2026-08-05&type=SALE_VOIDED&reference=V-000001&page=1&size=20',
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
