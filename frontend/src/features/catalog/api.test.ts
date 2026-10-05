import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  createCategory,
  createProduct,
  editProduct,
  findProductByCode,
  listCategories,
  listAdminProducts,
  renameCategory,
  searchProductsByName,
  setCategoryStatus,
  setProductStatus,
  type AdminProduct,
  type Category,
  type SaleProduct,
} from './api'

const category: Category = {
  id: 'category-1',
  name: 'Bebidas',
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

const saleProduct: SaleProduct = {
  id: product.id,
  code: product.code,
  name: product.name,
  categoryId: product.categoryId,
  categoryName: product.categoryName,
  salePrice: product.salePrice,
  currentStock: product.currentStock,
  available: product.available,
}

describe('catalog API', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('lists categories with the explicit inactive filter', async () => {
    const fetchMock = vi.fn().mockResolvedValueOnce(response([category]))
    vi.stubGlobal('fetch', fetchMock)

    await expect(listCategories(true)).resolves.toEqual([category])
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/admin/catalog/categories?includeInactive=true',
      { credentials: 'same-origin' },
    )
  })

  it('creates, renames and deactivates categories using CSRF', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(csrf('one'))
      .mockResolvedValueOnce(response(category))
      .mockResolvedValueOnce(csrf('two'))
      .mockResolvedValueOnce(response({ ...category, name: 'Jugos' }))
      .mockResolvedValueOnce(csrf('three'))
      .mockResolvedValueOnce(response({ ...category, status: 'INACTIVE' }))
    vi.stubGlobal('fetch', fetchMock)

    await createCategory('Bebidas')
    await renameCategory(category.id, 'Jugos')
    await setCategoryStatus(category.id, 'INACTIVE')

    expect(fetchMock).toHaveBeenNthCalledWith(
      2,
      '/api/v1/admin/catalog/categories',
      expect.objectContaining({ method: 'POST', body: JSON.stringify({ name: 'Bebidas' }) }),
    )
    expect(fetchMock).toHaveBeenNthCalledWith(
      4,
      '/api/v1/admin/catalog/categories/category-1',
      expect.objectContaining({ method: 'PUT', body: JSON.stringify({ name: 'Jugos' }) }),
    )
    expect(fetchMock).toHaveBeenNthCalledWith(
      6,
      '/api/v1/admin/catalog/categories/category-1/status',
      expect.objectContaining({
        method: 'PATCH',
        body: JSON.stringify({ status: 'INACTIVE' }),
      }),
    )
  })

  it('lists administrative products and searches sale products safely', async () => {
    const page = { items: [product], page: 0, size: 100, totalElements: 1, totalPages: 1 }
    const salePage = { ...page, items: [saleProduct], size: 20 }
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(response(page))
      .mockResolvedValueOnce(response(saleProduct))
      .mockResolvedValueOnce(response(salePage))
    vi.stubGlobal('fetch', fetchMock)

    await expect(listAdminProducts('café molido', true)).resolves.toEqual(page)
    await expect(findProductByCode(' 00123 ')).resolves.toEqual(saleProduct)
    await expect(searchProductsByName('café')).resolves.toEqual(salePage)

    expect(fetchMock.mock.calls[0][0]).toContain('name=caf%C3%A9+molido')
    expect(fetchMock.mock.calls[1][0]).toContain('code=+00123+')
    expect(fetchMock.mock.calls[2][0]).toContain('name=caf%C3%A9')
  })

  it('creates, edits and changes product status with explicit methods', async () => {
    const input = {
      code: '00123',
      name: 'Café',
      categoryId: category.id,
      purchasePrice: 1200,
      salePrice: 1590,
      minimumStock: 3,
    }
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(csrf('one'))
      .mockResolvedValueOnce(response(product))
      .mockResolvedValueOnce(csrf('two'))
      .mockResolvedValueOnce(response(product))
      .mockResolvedValueOnce(csrf('three'))
      .mockResolvedValueOnce(response({ ...product, status: 'INACTIVE' }))
    vi.stubGlobal('fetch', fetchMock)

    await createProduct(input)
    await editProduct(product.id, input)
    await setProductStatus(product.id, 'INACTIVE')

    expect(fetchMock.mock.calls[1][1]).toEqual(expect.objectContaining({ method: 'POST' }))
    expect(fetchMock.mock.calls[3][1]).toEqual(expect.objectContaining({ method: 'PUT' }))
    expect(fetchMock.mock.calls[5][1]).toEqual(expect.objectContaining({ method: 'PATCH' }))
  })
})

function csrf(token: string): Response {
  return response({ headerName: 'X-XSRF-TOKEN', token })
}

function response(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}
