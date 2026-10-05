import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  getInventorySnapshot,
  listInventoryProducts,
  recordInventoryAdjustment,
  recordInventoryEntry,
  type InventoryMovement,
  type InventorySnapshot,
} from './api'
import { InventoryAdmin } from './InventoryAdmin'

vi.mock('./api', () => ({
  getInventorySnapshot: vi.fn(),
  listInventoryProducts: vi.fn(),
  recordInventoryAdjustment: vi.fn(),
  recordInventoryEntry: vi.fn(),
}))

const movement: InventoryMovement = {
  id: 'movement-1',
  productId: 'product-1',
  type: 'ENTRY',
  quantity: 5,
  delta: 5,
  previousBalance: 0,
  resultingBalance: 5,
  reason: 'Compra inicial',
  reference: 'FAC-1',
  actorId: 'admin-1',
  actorDisplayName: 'Administración',
  occurredAt: '2026-08-04T04:00:00Z',
}

const snapshot: InventorySnapshot = {
  productId: 'product-1',
  productCode: '001',
  productName: 'Arroz',
  currentBalance: 5,
  version: 1,
  movements: { items: [movement], page: 0, size: 100, totalElements: 1, totalPages: 1 },
}

describe('InventoryAdmin', () => {
  beforeEach(() => {
    vi.mocked(listInventoryProducts)
      .mockReset()
      .mockResolvedValue([
        { id: 'product-1', code: '001', name: 'Arroz', status: 'ACTIVE', currentStock: 5 },
        { id: 'product-2', code: null, name: 'Azúcar', status: 'INACTIVE', currentStock: 0 },
      ])
    vi.mocked(getInventorySnapshot).mockReset().mockResolvedValue(snapshot)
    vi.mocked(recordInventoryEntry).mockReset().mockResolvedValue(movement)
    vi.mocked(recordInventoryAdjustment)
      .mockReset()
      .mockResolvedValue({ ...movement, type: 'ADJUSTMENT_OUT', quantity: 1, delta: -1 })
  })

  it('shows balance and immutable movement details', async () => {
    render(<InventoryAdmin />)

    expect(await screen.findByText('Compra inicial')).toBeInTheDocument()
    expect(screen.getByText('Saldo actual')).toBeInTheDocument()
    expect(screen.getByText('0 → 5')).toBeInTheDocument()
    expect(screen.getAllByText('Administración')).toHaveLength(2)
    expect(screen.queryByRole('button', { name: /editar|eliminar/i })).not.toBeInTheDocument()
  })

  it('requires evidence and then records an entry', async () => {
    const user = userEvent.setup()
    render(<InventoryAdmin />)
    await screen.findByText('Compra inicial')

    await user.type(screen.getByLabelText('Cantidad de entrada'), '3')
    await user.click(screen.getByRole('button', { name: 'Registrar entrada' }))
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'La entrada requiere un motivo o una referencia.',
    )
    expect(recordInventoryEntry).not.toHaveBeenCalled()

    await user.type(screen.getByLabelText('Referencia'), 'FAC-200')
    await user.click(screen.getByRole('button', { name: 'Registrar entrada' }))
    expect(recordInventoryEntry).toHaveBeenCalledWith('product-1', 3, '', 'FAC-200')
    expect(await screen.findByRole('status')).toHaveTextContent('Entrada registrada.')
    await waitFor(() => expect(getInventorySnapshot).toHaveBeenCalledTimes(2))
  })

  it('records a negative adjustment with its mandatory reason', async () => {
    const user = userEvent.setup()
    render(<InventoryAdmin />)
    await screen.findByText('Compra inicial')

    await user.selectOptions(screen.getByLabelText('Dirección'), 'NEGATIVE')
    await user.type(screen.getByLabelText('Cantidad del ajuste'), '2')
    await user.type(screen.getByLabelText('Motivo obligatorio'), 'Merma')
    await user.click(screen.getByRole('button', { name: 'Registrar ajuste' }))

    expect(recordInventoryAdjustment).toHaveBeenCalledWith('product-1', 'NEGATIVE', 2, 'Merma')
    expect(await screen.findByRole('status')).toHaveTextContent('Ajuste registrado.')
  })

  it('updates inventory from a physical count and records only the difference', async () => {
    const user = userEvent.setup()
    render(<InventoryAdmin />)
    await screen.findByText('Compra inicial')

    await user.type(screen.getByLabelText('Stock contado'), '3')
    await user.type(screen.getByLabelText('Motivo del conteo fisico'), 'Conteo de apertura')
    await user.click(screen.getByRole('button', { name: 'Actualizar al stock contado' }))

    expect(recordInventoryAdjustment).toHaveBeenCalledWith(
      'product-1',
      'NEGATIVE',
      2,
      'Conteo de apertura',
    )
    expect(await screen.findByRole('status')).toHaveTextContent(
      'Inventario actualizado seg\u00fan el conteo f\u00edsico.',
    )
  })

  it('loads a newly selected product and reports request failures safely', async () => {
    const user = userEvent.setup()
    vi.mocked(getInventorySnapshot)
      .mockResolvedValueOnce(snapshot)
      .mockRejectedValueOnce(new Error('Inventario no disponible.'))
    render(<InventoryAdmin />)
    await screen.findByText('Compra inicial')

    await user.selectOptions(screen.getByLabelText('Producto de inventario'), 'product-2')
    expect(await screen.findByRole('alert')).toHaveTextContent('Inventario no disponible.')
  })

  it('handles an empty catalog', async () => {
    const onOpenCatalog = vi.fn()
    const user = userEvent.setup()
    vi.mocked(listInventoryProducts).mockResolvedValueOnce([])
    render(<InventoryAdmin onOpenCatalog={onOpenCatalog} />)

    expect(await screen.findByText('No hay productos')).toBeInTheDocument()
    expect(screen.getByText('Primero crea un producto')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Ir a Productos' }))
    expect(onOpenCatalog).toHaveBeenCalledOnce()
    expect(screen.queryByText('Saldo actual')).not.toBeInTheDocument()
  })
})
