import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { findProductByCode, searchProductsByName, type SaleProduct } from './api'
import { ProductSearch } from './ProductSearch'

vi.mock('./api', () => ({
  findProductByCode: vi.fn(),
  searchProductsByName: vi.fn(),
}))

const product: SaleProduct = {
  id: 'product-1',
  code: '00123',
  name: 'Café Molido',
  categoryId: 'category-1',
  categoryName: 'Abarrotes',
  salePrice: 1590.5,
  currentStock: 0,
  available: false,
}

describe('ProductSearch', () => {
  beforeEach(() => {
    vi.mocked(findProductByCode).mockReset().mockResolvedValue(product)
    vi.mocked(searchProductsByName)
      .mockReset()
      .mockResolvedValue({
        items: [product],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      })
  })

  it('searches an exact code and marks zero stock unavailable', async () => {
    const user = userEvent.setup()
    render(<ProductSearch />)

    await user.type(screen.getByLabelText('Código exacto'), '00123')
    await user.click(screen.getByRole('button', { name: 'Buscar código' }))

    expect(findProductByCode).toHaveBeenCalledWith('00123')
    expect(await screen.findByText('Café Molido')).toBeInTheDocument()
    expect(screen.getByText('Sin stock · No disponible')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /agregar/i })).not.toBeInTheDocument()
  })

  it('searches by name, handles empty results and shows safe failures', async () => {
    const user = userEvent.setup()
    vi.mocked(searchProductsByName).mockResolvedValueOnce({
      items: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    })
    render(<ProductSearch />)

    await user.type(screen.getByLabelText('Nombre parcial'), 'café')
    await user.click(screen.getByRole('button', { name: 'Buscar nombre' }))
    expect(await screen.findByText('No se encontraron productos activos.')).toBeInTheDocument()

    vi.mocked(searchProductsByName).mockRejectedValueOnce(new Error('No fue posible buscar.'))
    await user.click(screen.getByRole('button', { name: 'Buscar nombre' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('No fue posible buscar.')
  })

  it('shows current availability returned by the backend', async () => {
    const user = userEvent.setup()
    vi.mocked(searchProductsByName).mockResolvedValueOnce({
      items: [{ ...product, available: true, currentStock: 4 }],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })
    render(<ProductSearch />)

    await user.type(screen.getByLabelText('Nombre parcial'), 'café')
    await user.click(screen.getByRole('button', { name: 'Buscar nombre' }))
    expect(await screen.findByText('Disponible: 4')).toBeInTheDocument()
  })
})
