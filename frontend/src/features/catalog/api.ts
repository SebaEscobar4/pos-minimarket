import { mutateJson, readJson } from '../../shared/api/client'

export type Category = {
  id: string
  name: string
  status: 'ACTIVE' | 'INACTIVE'
  createdAt: string
  updatedAt: string
}

export type ProductStatus = 'ACTIVE' | 'INACTIVE'

export type ProductInput = {
  code: string | null
  name: string
  categoryId: string
  purchasePrice: number
  salePrice: number
  minimumStock: number
}

export type AdminProduct = ProductInput & {
  id: string
  categoryName: string
  status: ProductStatus
  currentStock: number
  available: boolean
  createdAt: string
  updatedAt: string
}

export type SaleProduct = {
  id: string
  code: string | null
  name: string
  categoryId: string
  categoryName: string
  salePrice: number
  currentStock: number
  available: boolean
}

export type PageResponse<T> = {
  items: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export async function listCategories(includeInactive: boolean): Promise<Category[]> {
  return readJson<Category[]>(
    await fetch(`/api/v1/admin/catalog/categories?includeInactive=${includeInactive}`, {
      credentials: 'same-origin',
    }),
  )
}

export function createCategory(name: string): Promise<Category> {
  return mutateJson('/api/v1/admin/catalog/categories', 'POST', { name })
}

export function renameCategory(id: string, name: string): Promise<Category> {
  return mutateJson(`/api/v1/admin/catalog/categories/${id}`, 'PUT', { name })
}

export function setCategoryStatus(id: string, status: Category['status']): Promise<Category> {
  return mutateJson(`/api/v1/admin/catalog/categories/${id}/status`, 'PATCH', {
    status,
  })
}

export async function listAdminProducts(
  name: string,
  includeInactive: boolean,
): Promise<PageResponse<AdminProduct>> {
  const parameters = new URLSearchParams({
    includeInactive: String(includeInactive),
    page: '0',
    size: '100',
  })
  if (name.trim()) parameters.set('name', name)
  return readJson(
    await fetch(`/api/v1/admin/catalog/products?${parameters}`, {
      credentials: 'same-origin',
    }),
  )
}

export function createProduct(product: ProductInput): Promise<AdminProduct> {
  return mutateJson('/api/v1/admin/catalog/products', 'POST', product)
}

export function editProduct(id: string, product: ProductInput): Promise<AdminProduct> {
  return mutateJson(`/api/v1/admin/catalog/products/${id}`, 'PUT', product)
}

export function setProductStatus(id: string, status: ProductStatus): Promise<AdminProduct> {
  return mutateJson(`/api/v1/admin/catalog/products/${id}/status`, 'PATCH', { status })
}

export async function findProductByCode(code: string): Promise<SaleProduct> {
  const parameters = new URLSearchParams({ code })
  return readJson(
    await fetch(`/api/v1/catalog/products/by-code?${parameters}`, {
      credentials: 'same-origin',
    }),
  )
}

export async function searchProductsByName(name: string): Promise<PageResponse<SaleProduct>> {
  const parameters = new URLSearchParams({ name, page: '0', size: '20' })
  return readJson(
    await fetch(`/api/v1/catalog/products/search?${parameters}`, {
      credentials: 'same-origin',
    }),
  )
}
