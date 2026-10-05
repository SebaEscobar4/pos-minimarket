import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  closeCashSession,
  currentCashSession,
  currentCashSummary,
  openCashSession,
  recordManualCashMovement,
  type CashSession,
} from './api'

const session: CashSession = {
  id: 'cash-1',
  openedBy: 'seller-1',
  openedByDisplayName: 'Vendedor',
  openedAt: '2026-08-04T12:00:00Z',
  openingAmount: 25000,
  status: 'OPEN',
}

describe('cash API', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('reads the current cash session', async () => {
    const fetchMock = vi.fn().mockResolvedValueOnce(response({ session }))
    vi.stubGlobal('fetch', fetchMock)

    await expect(currentCashSession()).resolves.toEqual(session)
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/cash/sessions/current', {
      credentials: 'same-origin',
    })
  })

  it('opens the register through a CSRF-protected mutation', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(response({ headerName: 'X-XSRF-TOKEN', token: 'csrf' }))
      .mockResolvedValueOnce(response(session))
    vi.stubGlobal('fetch', fetchMock)

    await expect(openCashSession(25000)).resolves.toEqual(session)
    expect(fetchMock).toHaveBeenNthCalledWith(
      2,
      '/api/v1/cash/sessions',
      expect.objectContaining({ method: 'POST', body: JSON.stringify({ openingAmount: 25000 }) }),
    )
  })

  it('reads summary and writes manual movements and closure explicitly', async () => {
    const summary = {
      session,
      totals: {
        cashSales: 0,
        manualIncome: 5000,
        manualWithdrawals: 0,
        cashRefunds: 0,
        cardSales: 0,
        transferSales: 0,
      },
      expectedCash: 30000,
      movements: { items: [], page: 0, size: 100, totalElements: 0, totalPages: 0 },
    }
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(response(summary))
      .mockResolvedValueOnce(response({ headerName: 'X-XSRF-TOKEN', token: 'movement' }))
      .mockResolvedValueOnce(response({ id: 'movement-1' }))
      .mockResolvedValueOnce(response({ headerName: 'X-XSRF-TOKEN', token: 'close' }))
      .mockResolvedValueOnce(response({ sessionId: session.id, difference: 0 }))
    vi.stubGlobal('fetch', fetchMock)

    await expect(currentCashSummary()).resolves.toEqual(summary)
    await recordManualCashMovement('INCOME', 'CASH_REPLENISHMENT', 5000, 'Cambio')
    await closeCashSession(30000)

    expect(fetchMock.mock.calls[0][0]).toBe('/api/v1/cash/sessions/current/summary?page=0&size=100')
    expect(fetchMock).toHaveBeenNthCalledWith(
      3,
      '/api/v1/admin/cash/movements',
      expect.objectContaining({
        body: JSON.stringify({
          direction: 'INCOME',
          category: 'CASH_REPLENISHMENT',
          amount: 5000,
          reason: 'Cambio',
        }),
      }),
    )
    expect(fetchMock).toHaveBeenNthCalledWith(
      5,
      '/api/v1/cash/sessions/current/close',
      expect.objectContaining({ body: JSON.stringify({ countedCash: 30000 }) }),
    )
  })
})

function response(body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
  })
}
