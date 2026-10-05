import { afterEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { SaleWorkspace } from './SaleWorkspace'
import type { Sale } from './api'

describe('SaleWorkspace', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('adds an exact code, calculates change and renders the stored receipt', async () => {
    const user = userEvent.setup()
    vi.stubGlobal('crypto', { randomUUID: () => '10000000-0000-0000-0000-000000000001' })
    vi.stubGlobal(
      'fetch',
      vi
        .fn()
        .mockResolvedValueOnce(response(product()))
        .mockResolvedValueOnce(response({ headerName: 'X-XSRF-TOKEN', token: 'csrf' }))
        .mockResolvedValueOnce(response(sale(), 201)),
    )

    render(<SaleWorkspace role="SELLER" />)
    await user.type(screen.getByLabelText(/código exacto o parte del nombre/i), '001')
    await user.click(screen.getByRole('button', { name: 'Buscar' }))

    expect(await screen.findByText('Arroz')).toBeInTheDocument()
    await user.click(screen.getByLabelText('Cantidad de Arroz'))
    await user.keyboard('{Control>}a{/Control}2')
    expect(screen.getAllByText('$3.000')).toHaveLength(4)
    await user.type(screen.getByLabelText('Efectivo recibido'), '5000')
    expect(screen.getByText('$2.000')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Confirmar venta' }))

    expect(await screen.findByRole('heading', { name: 'Comprobante V-000001' })).toBeInTheDocument()
    expect(screen.getByText('Comprobante interno no tributario.')).toBeInTheDocument()
    expect(screen.getByText(/Venta V-000001 registrada correctamente/)).toBeInTheDocument()
    expect(fetch).toHaveBeenCalledTimes(3)
  })

  it('falls back to name results, validates stock and cancels the temporary cart', async () => {
    const user = userEvent.setup()
    vi.stubGlobal(
      'fetch',
      vi
        .fn()
        .mockResolvedValueOnce(response({ code: 'not-found', detail: 'No existe.' }, 404))
        .mockResolvedValueOnce(
          response({ items: [product()], page: 0, size: 20, totalElements: 1, totalPages: 1 }),
        ),
    )

    render(<SaleWorkspace role="SELLER" />)
    await user.type(screen.getByLabelText(/código exacto o parte del nombre/i), 'arro')
    await user.click(screen.getByRole('button', { name: 'Buscar' }))
    await user.click(await screen.findByRole('button', { name: 'Agregar' }))
    await user.clear(screen.getByLabelText('Cantidad de Arroz'))
    await user.type(screen.getByLabelText('Cantidad de Arroz'), '9')

    expect(screen.getByRole('alert')).toHaveTextContent('entre 1 y 5')
    await user.click(screen.getByRole('button', { name: 'Cancelar compra' }))
    expect(screen.getByText('Todavía no agregaste productos.')).toBeInTheDocument()
  })

  it('shows the administrator sale log and its product detail', async () => {
    const user = userEvent.setup()
    vi.stubGlobal(
      'fetch',
      vi
        .fn()
        .mockResolvedValue(
          response({ items: [sale()], page: 0, size: 20, totalElements: 1, totalPages: 1 }),
        ),
    )

    render(<SaleWorkspace role="ADMIN" />)
    expect(await screen.findByRole('heading', { name: 'Ventas recientes' })).toBeInTheDocument()
    await waitFor(() => expect(screen.getByText('V-000001')).toBeInTheDocument())
    await user.click(screen.getByRole('button', { name: 'Ver detalle' }))

    const detail = screen.getByRole('heading', { name: 'Detalle V-000001' }).parentElement
    expect(detail).not.toBeNull()
    expect(within(detail!).getByText('Arroz')).toBeInTheDocument()
    expect(within(detail!).getAllByText('$3.000')).toHaveLength(3)
  })

  it('lets the administrator confirm a cancellation from sale history', async () => {
    const user = userEvent.setup()
    vi.stubGlobal(
      'fetch',
      vi
        .fn()
        .mockResolvedValueOnce(
          response({ items: [sale()], page: 0, size: 20, totalElements: 1, totalPages: 1 }),
        )
        .mockResolvedValueOnce(response({ headerName: 'X-XSRF-TOKEN', token: 'csrf' }))
        .mockResolvedValueOnce(response(voidedSale(), 201)),
    )

    render(<SaleWorkspace role="ADMIN" />)
    await user.click(await screen.findByRole('button', { name: 'Ver detalle' }))
    await user.click(screen.getByRole('button', { name: 'Anular venta' }))
    await user.type(screen.getByLabelText('Motivo'), 'Cobro duplicado')
    await user.click(screen.getByRole('button', { name: 'Confirmar anulación' }))

    expect(await screen.findByText('Venta anulada')).toBeInTheDocument()
    expect(screen.getByText(/Venta V-000001 anulada correctamente/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Anular venta' })).not.toBeInTheDocument()
  })

  it('rejects an empty cart and insufficient cash before calling the backend', async () => {
    const user = userEvent.setup()
    const fetchMock = vi.fn().mockResolvedValueOnce(response(product()))
    vi.stubGlobal('fetch', fetchMock)

    render(<SaleWorkspace role="SELLER" />)
    expect(screen.getByRole('button', { name: 'Confirmar venta' })).toBeDisabled()
    await user.type(screen.getByLabelText(/código exacto o parte del nombre/i), '001')
    await user.click(screen.getByRole('button', { name: 'Buscar' }))
    await user.type(screen.getByLabelText('Efectivo recibido'), '100')
    await user.click(screen.getByRole('button', { name: 'Confirmar venta' }))

    expect(screen.getByRole('alert')).toHaveTextContent('igual o superior al total efectivo')
    expect(fetchMock).toHaveBeenCalledTimes(1)
  })

  it('shows the Chilean legal rounding only for a cash payment', async () => {
    const user = userEvent.setup()
    vi.stubGlobal('fetch', vi.fn().mockResolvedValueOnce(response(product(498))))

    render(<SaleWorkspace role="SELLER" />)
    await user.type(screen.getByLabelText(/código exacto o parte del nombre/i), '001')
    await user.click(screen.getByRole('button', { name: 'Buscar' }))
    await user.type(screen.getByLabelText('Efectivo recibido'), '500')

    expect(screen.getByText('Total de la venta').parentElement).toHaveTextContent('$498')
    expect(screen.getByText('Redondeo efectivo').parentElement).toHaveTextContent('+$2')
    expect(screen.getByText('Total efectivo').parentElement).toHaveTextContent('$500')
    expect(screen.getByText('Vuelto').parentElement).toHaveTextContent('$0')
  })
})

function product(salePrice = 1500) {
  return {
    id: 'product-1',
    code: '001',
    name: 'Arroz',
    categoryId: 'category-1',
    categoryName: 'Abarrotes',
    salePrice,
    currentStock: 5,
    available: true,
  }
}

function sale(): Sale {
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

function voidedSale(): Sale {
  return {
    ...sale(),
    status: 'VOIDED',
    cancellation: {
      refundMethod: 'CASH',
      amount: 3000,
      cashPayable: 3000,
      roundingAdjustment: 0,
      reason: 'Cobro duplicado',
      actorId: 'admin-1',
      actorDisplayName: 'Administración',
      occurredAt: '2026-08-05T16:00:00Z',
    },
  }
}

function response(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}
