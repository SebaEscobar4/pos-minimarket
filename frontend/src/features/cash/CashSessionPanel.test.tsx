import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  closeCashSession,
  currentCashSession,
  currentCashSummary,
  openCashSession,
  recordManualCashMovement,
  type CashSession,
  type CashSummary,
} from './api'
import { CashSessionPanel } from './CashSessionPanel'

vi.mock('./api', () => ({
  closeCashSession: vi.fn(),
  currentCashSession: vi.fn(),
  currentCashSummary: vi.fn(),
  openCashSession: vi.fn(),
  recordManualCashMovement: vi.fn(),
}))

const session: CashSession = {
  id: 'cash-1',
  openedBy: 'seller-1',
  openedByDisplayName: 'Vendedor',
  openedAt: '2026-08-04T12:00:00Z',
  openingAmount: 25000,
  status: 'OPEN',
}

const summary: CashSummary = {
  session,
  totals: {
    cashSales: 10000,
    manualIncome: 5000,
    manualWithdrawals: 3000,
    cashRefunds: 1000,
    cardSales: 7000,
    transferSales: 6000,
  },
  expectedCash: 36000,
  movements: {
    items: [
      {
        id: 'movement-1',
        cashSessionId: session.id,
        type: 'MANUAL_INCOME',
        category: 'CASH_REPLENISHMENT',
        amount: 5000,
        reason: 'Refuerzo para cambio',
        reference: null,
        actorId: 'admin-1',
        actorDisplayName: 'Administración',
        occurredAt: '2026-08-04T13:00:00Z',
      },
    ],
    page: 0,
    size: 100,
    totalElements: 1,
    totalPages: 1,
  },
}

describe('CashSessionPanel', () => {
  beforeEach(() => {
    vi.mocked(currentCashSession).mockReset().mockResolvedValue(null)
    vi.mocked(currentCashSummary).mockReset().mockResolvedValue(summary)
    vi.mocked(openCashSession).mockReset().mockResolvedValue(session)
    vi.mocked(recordManualCashMovement).mockReset().mockResolvedValue(summary.movements.items[0])
    vi.mocked(closeCashSession).mockReset().mockResolvedValue({
      sessionId: session.id,
      closedBy: 'seller-1',
      closedByDisplayName: 'Vendedor',
      closedAt: '2026-08-04T18:00:00Z',
      openingAmount: 25000,
      expectedCash: 36000,
      countedCash: 35500,
      difference: -500,
      totals: summary.totals,
    })
  })

  it('opens a register with an exact whole-peso amount', async () => {
    const user = userEvent.setup()
    render(<CashSessionPanel role="SELLER" />)

    await user.type(await screen.findByLabelText('Monto inicial en pesos'), '25000')
    await user.click(screen.getByRole('button', { name: 'Abrir caja' }))

    expect(openCashSession).toHaveBeenCalledWith(25000)
    expect(await screen.findByText('Abierta')).toBeInTheDocument()
    expect(screen.getByText(/Abierta por Vendedor/)).toBeInTheDocument()
  })

  it('shows reconciliable totals to a seller without manual movement controls', async () => {
    vi.mocked(currentCashSession).mockResolvedValueOnce(session)
    render(<CashSessionPanel role="SELLER" />)

    expect(await screen.findByText('Efectivo esperado')).toBeInTheDocument()
    expect(screen.getByText('Ventas con tarjeta')).toBeInTheDocument()
    expect(screen.getByText('Ventas por transferencia')).toBeInTheDocument()
    expect(screen.getByText('Refuerzo para cambio')).toBeInTheDocument()
    expect(screen.queryByText('Movimiento manual')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Cerrar caja' })).toBeInTheDocument()
  })

  it('lets an administrator record only approved categorized movements', async () => {
    const user = userEvent.setup()
    vi.mocked(currentCashSession).mockResolvedValueOnce(session)
    render(<CashSessionPanel role="ADMIN" />)
    await screen.findByText('Movimiento manual')

    await user.selectOptions(screen.getByLabelText('Tipo de movimiento de caja'), 'WITHDRAWAL')
    await user.selectOptions(screen.getByLabelText('Categoría'), 'SUPPLIER_PAYMENT')
    await user.type(screen.getByLabelText('Monto'), '3000')
    await user.type(screen.getByLabelText('Motivo obligatorio'), 'Pago de mercadería')
    await user.click(screen.getByRole('button', { name: 'Registrar movimiento' }))

    expect(recordManualCashMovement).toHaveBeenCalledWith(
      'WITHDRAWAL',
      'SUPPLIER_PAYMENT',
      3000,
      'Pago de mercadería',
    )
    expect(await screen.findByRole('status')).toHaveTextContent('Movimiento de caja registrado.')
    await waitFor(() => expect(currentCashSummary).toHaveBeenCalledTimes(2))
  })

  it('closes the register and preserves the counted difference', async () => {
    const user = userEvent.setup()
    vi.mocked(currentCashSession).mockResolvedValueOnce(session)
    render(<CashSessionPanel role="SELLER" />)
    await screen.findByText('Efectivo esperado')

    await user.type(screen.getByLabelText('Efectivo contado'), '35500')
    await user.click(screen.getByRole('button', { name: 'Cerrar caja' }))

    expect(closeCashSession).toHaveBeenCalledWith(35500)
    expect(await screen.findByText('Cierre registrado')).toBeInTheDocument()
    expect(screen.getByText(/Diferencia conservada/)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Continuar' }))
    expect(await screen.findByRole('button', { name: 'Abrir caja' })).toBeInTheDocument()
  })

  it('reports loading and opening failures safely', async () => {
    vi.mocked(currentCashSession).mockRejectedValueOnce(new Error('Caja no disponible.'))
    const { unmount } = render(<CashSessionPanel role="SELLER" />)
    expect(await screen.findByRole('alert')).toHaveTextContent('Caja no disponible.')
    unmount()

    vi.mocked(currentCashSession).mockResolvedValueOnce(null)
    vi.mocked(openCashSession).mockRejectedValueOnce(new Error('Ya existe una caja abierta.'))
    const user = userEvent.setup()
    render(<CashSessionPanel role="SELLER" />)
    await user.type(await screen.findByLabelText('Monto inicial en pesos'), '10000')
    await user.click(screen.getByRole('button', { name: 'Abrir caja' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Ya existe una caja abierta.')
  })
})
