import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { listAuditEvents, type AuditEventPage } from './api'
import { AuditWorkspace } from './AuditWorkspace'

vi.mock('./api', () => ({ listAuditEvents: vi.fn() }))

const page: AuditEventPage = {
  items: [
    {
      id: 'SALE-VOID-1',
      type: 'SALE_VOIDED',
      occurredAt: '2026-08-05T20:00:00Z',
      actorId: 'admin-1',
      actorDisplayName: 'Administraci\u00f3n',
      reference: 'V-000001',
      summary: 'Venta anulada por $406. Motivo: Cobro duplicado',
    },
  ],
  page: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
}

describe('AuditWorkspace', () => {
  beforeEach(() => {
    vi.mocked(listAuditEvents).mockReset().mockResolvedValue(page)
  })

  it('shows immutable critical operation details', async () => {
    render(<AuditWorkspace />)

    expect(await screen.findByText('Venta anulada')).toBeInTheDocument()
    expect(screen.getByText('V-000001')).toBeInTheDocument()
    expect(screen.getByText('Administraci\u00f3n')).toBeInTheDocument()
    expect(screen.getByText(/Cobro duplicado/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /editar|eliminar/i })).not.toBeInTheDocument()
  })

  it('applies date, type and reference filters', async () => {
    const user = userEvent.setup()
    render(<AuditWorkspace />)
    await screen.findByText('Venta anulada')

    await user.type(screen.getByLabelText('Desde'), '2026-08-01')
    await user.type(screen.getByLabelText('Hasta'), '2026-08-05')
    await user.selectOptions(screen.getByLabelText('Tipo de evento'), 'SALE_VOIDED')
    await user.type(screen.getByLabelText('Referencia'), 'V-000001')
    await user.click(screen.getByRole('button', { name: 'Aplicar filtros' }))

    await waitFor(() =>
      expect(listAuditEvents).toHaveBeenLastCalledWith({
        from: '2026-08-01',
        to: '2026-08-05',
        type: 'SALE_VOIDED',
        reference: 'V-000001',
        page: 0,
        size: 20,
      }),
    )
  })
})
