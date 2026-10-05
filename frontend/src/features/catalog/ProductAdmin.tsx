import { FormEvent, useCallback, useEffect, useState } from 'react'
import {
  createProduct,
  editProduct,
  listAdminProducts,
  listCategories,
  setProductStatus,
  type AdminProduct,
  type Category,
  type ProductInput,
} from './api'

type Draft = {
  code: string
  name: string
  categoryId: string
  purchasePrice: string
  salePrice: string
  minimumStock: string
}

const emptyDraft: Draft = {
  code: '',
  name: '',
  categoryId: '',
  purchasePrice: '0',
  salePrice: '0',
  minimumStock: '0',
}

export function ProductAdmin({ refreshToken = 0 }: { refreshToken?: number }) {
  const [products, setProducts] = useState<AdminProduct[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [draft, setDraft] = useState<Draft>(emptyDraft)
  const [editing, setEditing] = useState<AdminProduct | null>(null)
  const [filterText, setFilterText] = useState('')
  const [appliedFilter, setAppliedFilter] = useState('')
  const [includeInactive, setIncludeInactive] = useState(false)
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  const refresh = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const page = await listAdminProducts(appliedFilter, includeInactive)
      setProducts(page.items)
    } catch (reason) {
      setError(messageFrom(reason))
    } finally {
      setLoading(false)
    }
  }, [appliedFilter, includeInactive])

  useEffect(() => {
    let active = true
    void Promise.all([listCategories(false), listAdminProducts(appliedFilter, includeInactive)])
      .then(([categoryResult, productPage]) => {
        if (!active) return
        setCategories(categoryResult)
        setProducts(productPage.items)
      })
      .catch((reason: unknown) => {
        if (active) setError(messageFrom(reason))
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [appliedFilter, includeInactive, refreshToken])

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSubmitting(true)
    setError('')
    try {
      const input = toProductInput(draft)
      if (editing) await editProduct(editing.id, input)
      else await createProduct(input)
      cancelEditing()
      await refresh()
    } catch (reason) {
      setError(messageFrom(reason))
    } finally {
      setSubmitting(false)
    }
  }

  async function changeStatus(product: AdminProduct) {
    setError('')
    try {
      await setProductStatus(product.id, product.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE')
      await refresh()
    } catch (reason) {
      setError(messageFrom(reason))
    }
  }

  function startEditing(product: AdminProduct) {
    setEditing(product)
    setDraft({
      code: product.code ?? '',
      name: product.name,
      categoryId: product.categoryId,
      purchasePrice: String(product.purchasePrice),
      salePrice: String(product.salePrice),
      minimumStock: String(product.minimumStock),
    })
  }

  function cancelEditing() {
    setEditing(null)
    setDraft(emptyDraft)
  }

  function applySearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setLoading(true)
    setAppliedFilter(filterText)
  }

  return (
    <section className="catalog-card" aria-labelledby="product-admin-title">
      <div className="section-heading">
        <div>
          <p className="eyebrow">Administración</p>
          <h2 id="product-admin-title">Productos</h2>
        </div>
        <label className="inline-control">
          <input
            type="checkbox"
            checked={includeInactive}
            onChange={(event) => {
              setLoading(true)
              setIncludeInactive(event.target.checked)
            }}
          />
          Incluir productos inactivos
        </label>
      </div>

      <form className="filter-form" onSubmit={applySearch}>
        <label htmlFor="admin-product-filter">Filtrar por nombre</label>
        <div className="form-row">
          <input
            id="admin-product-filter"
            value={filterText}
            onChange={(event) => setFilterText(event.target.value)}
            maxLength={150}
          />
          <button type="submit">Filtrar</button>
        </div>
      </form>

      <form className="product-form" onSubmit={(event) => void submit(event)}>
        <div className="field-grid">
          <label>
            Código opcional
            <input
              aria-label="Código del producto"
              value={draft.code}
              onChange={(event) => setDraft({ ...draft, code: event.target.value })}
              maxLength={64}
              pattern="[-A-Za-z0-9._/]+"
              title="Letras, números o - . _ /"
            />
          </label>
          <label>
            Nombre
            <input
              aria-label="Nombre del producto"
              value={draft.name}
              onChange={(event) => setDraft({ ...draft, name: event.target.value })}
              maxLength={150}
              required
            />
          </label>
          <label>
            Categoría
            <select
              aria-label="Categoría del producto"
              value={draft.categoryId}
              onChange={(event) => setDraft({ ...draft, categoryId: event.target.value })}
              required
            >
              <option value="">Selecciona una categoría</option>
              {categories.map((category) => (
                <option key={category.id} value={category.id}>
                  {category.name}
                </option>
              ))}
            </select>
          </label>
          <MoneyField
            label="Precio de compra"
            value={draft.purchasePrice}
            onChange={(purchasePrice) => setDraft({ ...draft, purchasePrice })}
          />
          <MoneyField
            label="Precio de venta"
            value={draft.salePrice}
            onChange={(salePrice) => setDraft({ ...draft, salePrice })}
          />
          <label>
            Stock mínimo
            <input
              aria-label="Stock mínimo"
              type="number"
              min="0"
              max="1000000"
              step="1"
              value={draft.minimumStock}
              onChange={(event) => setDraft({ ...draft, minimumStock: event.target.value })}
              required
            />
          </label>
        </div>
        <div className="form-row">
          <button type="submit" disabled={submitting || categories.length === 0}>
            {editing ? 'Guardar producto' : 'Crear producto'}
          </button>
          {editing && (
            <button type="button" className="secondary-button" onClick={cancelEditing}>
              Cancelar edición
            </button>
          )}
        </div>
        <p className="form-help">
          El stock no se modifica aquí; todo producto nuevo parte en cero.
        </p>
      </form>

      {error && (
        <p className="error-message" role="alert">
          {error}
        </p>
      )}
      {loading ? (
        <p role="status">Cargando productos…</p>
      ) : products.length === 0 ? (
        <p>No hay productos para mostrar.</p>
      ) : (
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th scope="col">Producto</th>
                <th scope="col">Venta</th>
                <th scope="col">Stock</th>
                <th scope="col">Estado</th>
                <th scope="col">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {products.map((product) => (
                <tr key={product.id}>
                  <td>
                    <strong>{product.name}</strong>
                    <br />
                    <small>
                      {product.code ?? 'Sin código'} · {product.categoryName}
                    </small>
                  </td>
                  <td>${formatMoney(product.salePrice)}</td>
                  <td>{product.currentStock}</td>
                  <td>{product.status === 'ACTIVE' ? 'Activo' : 'Inactivo'}</td>
                  <td className="actions-cell">
                    <button
                      type="button"
                      className="secondary-button compact-button"
                      onClick={() => startEditing(product)}
                    >
                      Editar
                    </button>
                    <button
                      type="button"
                      className="secondary-button compact-button"
                      onClick={() => void changeStatus(product)}
                    >
                      {product.status === 'ACTIVE' ? 'Desactivar' : 'Activar'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}

function MoneyField({
  label,
  value,
  onChange,
}: {
  label: string
  value: string
  onChange: (value: string) => void
}) {
  return (
    <label>
      {label}
      <input
        aria-label={label}
        type="number"
        min="0"
        max="9999999999.99"
        step="0.01"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        required
      />
    </label>
  )
}

function toProductInput(draft: Draft): ProductInput {
  return {
    code: draft.code.trim() || null,
    name: draft.name,
    categoryId: draft.categoryId,
    purchasePrice: Number(draft.purchasePrice),
    salePrice: Number(draft.salePrice),
    minimumStock: Number(draft.minimumStock),
  }
}

function formatMoney(value: number): string {
  return new Intl.NumberFormat('es-CL', { maximumFractionDigits: 2 }).format(value)
}

function messageFrom(reason: unknown): string {
  return reason instanceof Error ? reason.message : 'Ocurrió un error inesperado.'
}
