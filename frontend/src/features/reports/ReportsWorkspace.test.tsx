import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getCashReport, getInventoryReport, getSalesReport, type SalesReport } from './api'
import { ReportsWorkspace } from './ReportsWorkspace'

vi.mock('./api', () => ({
  getSalesReport: vi.fn(),
  getInventoryReport: vi.fn(),
  getCashReport: vi.fn(),
}))

const SALES_REPORT: SalesReport = {
  from: '2026-08-01',
  to: '2026-08-05',
  methodFilter: null,
  recordedSales: 4,
  recordedAmount: 1015,
  voidedSales: 3,
  voidedAmount: 812,
  netSales: 1,
  netAmount: 203,
  estimatedGrossProfit: 103,
  payments: [
    {
      method: 'CASH',
      recordedSales: 2,
      recordedAmount: 609,
      voidedSales: 1,
      voidedAmount: 406,
      netSales: 1,
      netAmount: 203,
    },
  ],
  refunds: [],
  topProducts: [
    {
      productId: 'p1',
      code: 'SALE-001',
      name: 'Producto para venta',
      quantity: 1,
      revenue: 203,
    },
  ],
}

describe('ReportsWorkspace', () => {
  beforeEach(() => {
    vi.mocked(getSalesReport).mockReset().mockResolvedValue(SALES_REPORT)
    vi.mocked(getInventoryReport)
      .mockReset()
      .mockResolvedValue({
        from: '2026-08-01',
        to: '2026-08-05',
        typeFilter: null,
        productFilter: null,
        lowStock: [
          {
            productId: 'p2',
            code: 'BAJO',
            name: 'Producto bajo',
            quantity: 1,
            minimumStock: 3,
            shortage: 2,
          },
        ],
        totals: [{ type: 'ENTRY', movements: 1, quantity: 5, netDelta: 5 }],
        movements: {
          items: [
            {
              id: 'm1',
              productId: 'p2',
              productCode: 'BAJO',
              productName: 'Producto bajo',
              type: 'ADJUSTMENT_OUT',
              quantity: 2,
              delta: -2,
              previousBalance: 3,
              resultingBalance: 1,
              reason: 'Conteo de inventario',
              reference: null,
              actorDisplayName: 'Administraci\u00f3n',
              occurredAt: '2026-08-05T12:00:00Z',
            },
          ],
          page: 0,
          size: 20,
          totalElements: 1,
          totalPages: 1,
        },
      })
    vi.mocked(getCashReport)
      .mockReset()
      .mockResolvedValue({
        from: '2026-08-01',
        to: '2026-08-05',
        openedSessions: 2,
        closedSessions: 1,
        openingAmount: 20000,
        expectedCash: 10410,
        countedCash: 10000,
        difference: -410,
        sessions: {
          items: [
            {
              id: 'c1',
              status: 'OPEN',
              openedBy: 'Administraci\u00f3n',
              openedAt: '2026-08-05T12:00:00Z',
              openingAmount: 10000,
              closedBy: null,
              closedAt: null,
              expectedCash: null,
              countedCash: null,
              difference: null,
            },
            {
              id: 'c2',
              status: 'CLOSED',
              openedBy: 'Administraci\u00f3n',
              openedAt: '2026-08-04T12:00:00Z',
              openingAmount: 10000,
              closedBy: 'Administraci\u00f3n',
              closedAt: '2026-08-04T20:00:00Z',
              expectedCash: 10410,
              countedCash: 10000,
              difference: -410,
            },
          ],
          page: 0,
          size: 20,
          totalElements: 2,
          totalPages: 1,
        },
      })
  })

  it('shows reconciled sales and top products', async () => {
    render(<ReportsWorkspace />)

    expect(await screen.findByText('Ventas registradas')).toBeInTheDocument()
    expect(screen.getByText('Venta neta')).toBeInTheDocument()
    expect(screen.getByText('Producto para venta')).toBeInTheDocument()
    expect(screen.getByText('SALE-001')).toBeInTheDocument()
  })

  it('switches to inventory and cash reports', async () => {
    const user = userEvent.setup()
    render(<ReportsWorkspace />)
    await screen.findByText('Ventas registradas')

    await user.click(screen.getByRole('button', { name: 'Inventario' }))
    expect(await screen.findAllByText('Producto bajo')).toHaveLength(2)
    expect(screen.getByText('Libro de inventario')).toBeInTheDocument()
    expect(screen.getByText('Conteo de inventario')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Caja' }))
    expect(await screen.findByText('Aperturas')).toBeInTheDocument()
    expect(screen.getByText('Diferencia acumulada')).toBeInTheDocument()
    expect(screen.getByText('Abierta')).toBeInTheDocument()
    expect(screen.getByText('Cerrada')).toBeInTheDocument()
  })

  it('applies payment and date filters and renders an empty sales period', async () => {
    const user = userEvent.setup()
    render(<ReportsWorkspace />)
    await screen.findByText('Producto para venta')
    vi.mocked(getSalesReport).mockResolvedValue({ ...SALES_REPORT, topProducts: [] })

    await user.selectOptions(screen.getByLabelText('Medio de pago'), 'CASH')
    expect(await screen.findByText('No hay ventas netas en el per\u00edodo.')).toBeInTheDocument()
    expect(getSalesReport).toHaveBeenLastCalledWith(expect.objectContaining({ method: 'CASH' }))

    await user.clear(screen.getByLabelText('Desde'))
    await user.type(screen.getByLabelText('Desde'), '2026-08-01')
    await user.clear(screen.getByLabelText('Hasta'))
    await user.type(screen.getByLabelText('Hasta'), '2026-08-05')
    await user.click(screen.getByRole('button', { name: 'Actualizar reportes' }))
    expect(getSalesReport).toHaveBeenLastCalledWith({
      from: '2026-08-01',
      to: '2026-08-05',
      method: 'CASH',
    })
  })

  it('shows a safe API failure', async () => {
    vi.mocked(getSalesReport).mockRejectedValueOnce(new Error('No se pudo cargar el reporte.'))

    render(<ReportsWorkspace />)

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo cargar el reporte.')
  })
})
