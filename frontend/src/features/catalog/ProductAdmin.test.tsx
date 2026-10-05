import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  createProduct,
  editProduct,
  listAdminProducts,
  listCategories,
  setProductStatus,
  type AdminProduct,
  type Category,
} from './api'
import { ProductAdmin } from './ProductAdmin'

vi.mock('./api', () => ({
  createProduct: vi.fn(),
  editProduct: vi.fn(),
  listAdminProducts: vi.fn(),
  listCategories: vi.fn(),
  setProductStatus: vi.fn(),
}))

const category: Category = {
  id: 'category-1',
  name: 'Abarrotes',
  status: 'ACTIVE',
  createdAt: '2026-08-04T04:00:00Z',
  updatedAt: '2026-08-04T04:00:00Z',
}

const product: AdminProduct = {
  id: 'product-1',
  code: '00123',
  name: 'Café',
  categoryId: category.id,
  categoryName: category.name,
  purchasePrice: 1200,
  salePrice: 1590,
  minimumStock: 3,
  status: 'ACTIVE',
  currentStock: 0,
  available: false,
  createdAt: '2026-08-04T04:00:00Z',
  updatedAt: '2026-08-04T04:00:00Z',
}

describe('ProductAdmin', () => {
  beforeEach(() => {
    vi.mocked(listCategories).mockReset().mockResolvedValue([category])
    vi.mocked(listAdminProducts)
      .mockReset()
      .mockResolvedValue(page([product]))
    vi.mocked(createProduct).mockReset().mockResolvedValue(product)
    vi.mocked(editProduct).mockReset().mockResolvedValue(product)
    vi.mocked(setProductStatus)
      .mockReset()
      .mockResolvedValue({
        ...product,
        status: 'INACTIVE',
      })
  })

  it('creates, edits and deactivates products without a stock input', async () => {
    const user = userEvent.setup()
    render(<ProductAdmin />)

    expect(await screen.findByText('Café')).toBeInTheDocument()
    expect(screen.queryByLabelText('Stock actual')).not.toBeInTheDocument()
    await user.type(screen.getByLabelText('Nombre del producto'), 'Té')
    await user.selectOptions(screen.getByLabelText('Categoría del producto'), category.id)
    await user.clear(screen.getByLabelText('Precio de compra'))
    await user.type(screen.getByLabelText('Precio de compra'), '500')
    await user.clear(screen.getByLabelText('Precio de venta'))
    await user.type(screen.getByLabelText('Precio de venta'), '700')
    await user.click(screen.getByRole('button', { name: 'Crear producto' }))
    expect(createProduct).toHaveBeenCalledWith(
      expect.objectContaining({ code: null, name: 'Té', categoryId: category.id }),
    )

    await user.click(screen.getByRole('button', { name: 'Editar' }))
    await user.clear(screen.getByLabelText('Nombre del producto'))
    await user.type(screen.getByLabelText('Nombre del producto'), 'Café Premium')
    await user.click(screen.getByRole('button', { name: 'Guardar producto' }))
    expect(editProduct).toHaveBeenCalledWith(
      product.id,
      expect.objectContaining({ code: '00123', name: 'Café Premium' }),
    )

    await user.click(screen.getByRole('button', { name: 'Desactivar' }))
    expect(setProductStatus).toHaveBeenCalledWith(product.id, 'INACTIVE')
  })

  it('filters, includes inactive products and reports request failures', async () => {
    const user = userEvent.setup()
    render(<ProductAdmin />)

    await screen.findByText('Café')
    await user.type(screen.getByLabelText('Filtrar por nombre'), 'café')
    await user.click(screen.getByRole('button', { name: 'Filtrar' }))
    await waitFor(() => expect(listAdminProducts).toHaveBeenCalledWith('café', false))

    await user.click(screen.getByLabelText('Incluir productos inactivos'))
    await waitFor(() => expect(listAdminProducts).toHaveBeenLastCalledWith('café', true))

    vi.mocked(setProductStatus).mockRejectedValueOnce(new Error('Operación rechazada.'))
    await user.click(screen.getByRole('button', { name: 'Desactivar' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Operación rechazada.')
  })

  it('cancels edits and activates an inactive product', async () => {
    const user = userEvent.setup()
    vi.mocked(listAdminProducts).mockResolvedValue(page([{ ...product, status: 'INACTIVE' }]))
    render(<ProductAdmin />)

    await screen.findByText('Café')
    await user.click(screen.getByRole('button', { name: 'Editar' }))
    await user.click(screen.getByRole('button', { name: 'Cancelar edición' }))
    expect(screen.getByLabelText('Nombre del producto')).toHaveValue('')

    await user.click(screen.getByRole('button', { name: 'Activar' }))
    expect(setProductStatus).toHaveBeenCalledWith(product.id, 'ACTIVE')
  })

  it('reports initial loading failures without exposing internals', async () => {
    vi.mocked(listCategories).mockRejectedValueOnce(new Error('Catálogo no disponible.'))
    render(<ProductAdmin />)

    expect(await screen.findByRole('alert')).toHaveTextContent('Catálogo no disponible.')
  })
})

function page(items: AdminProduct[]) {
  return {
    items,
    page: 0,
    size: 100,
    totalElements: items.length,
    totalPages: items.length ? 1 : 0,
  }
}
