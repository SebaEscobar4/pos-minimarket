import { FormEvent, useState } from 'react'
import { findProductByCode, searchProductsByName, type SaleProduct } from './api'

export function ProductSearch() {
  const [results, setResults] = useState<SaleProduct[]>([])
  const [searched, setSearched] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  async function searchCode(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const data = new FormData(event.currentTarget)
    await search(async () => [await findProductByCode(field(data, 'code'))])
  }

  async function searchName(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const data = new FormData(event.currentTarget)
    await search(async () => (await searchProductsByName(field(data, 'name'))).items)
  }

  async function search(request: () => Promise<SaleProduct[]>) {
    setLoading(true)
    setError('')
    try {
      setResults(await request())
    } catch (reason) {
      setResults([])
      setError(messageFrom(reason))
    } finally {
      setSearched(true)
      setLoading(false)
    }
  }

  return (
    <section className="catalog-card" aria-labelledby="product-search-title">
      <p className="eyebrow">Punto de venta</p>
      <h2 id="product-search-title">Buscar productos</h2>
      <div className="search-grid">
        <form onSubmit={(event) => void searchCode(event)}>
          <label htmlFor="sale-product-code">Código exacto</label>
          <div className="form-row">
            <input id="sale-product-code" name="code" maxLength={66} required />
            <button type="submit" disabled={loading}>
              Buscar código
            </button>
          </div>
        </form>
        <form onSubmit={(event) => void searchName(event)}>
          <label htmlFor="sale-product-name">Nombre parcial</label>
          <div className="form-row">
            <input id="sale-product-name" name="name" maxLength={150} required />
            <button type="submit" disabled={loading}>
              Buscar nombre
            </button>
          </div>
        </form>
      </div>
      {error && (
        <p className="error-message" role="alert">
          {error}
        </p>
      )}
      {!error && searched && results.length === 0 && <p>No se encontraron productos activos.</p>}
      {results.length > 0 && (
        <div className="product-results" aria-live="polite">
          {results.map((product) => (
            <article className="product-result" key={product.id}>
              <div>
                <h3>{product.name}</h3>
                <p>
                  {product.categoryName} · Código: {product.code ?? 'Sin código'}
                </p>
              </div>
              <div className="product-numbers">
                <strong>${formatMoney(product.salePrice)}</strong>
                <span>
                  {product.available
                    ? `Disponible: ${product.currentStock}`
                    : 'Sin stock · No disponible'}
                </span>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  )
}

function field(data: FormData, name: string): string {
  const value = data.get(name)
  return typeof value === 'string' ? value : ''
}

function formatMoney(value: number): string {
  return new Intl.NumberFormat('es-CL', { maximumFractionDigits: 2 }).format(value)
}

function messageFrom(reason: unknown): string {
  return reason instanceof Error ? reason.message : 'Ocurrió un error inesperado.'
}
